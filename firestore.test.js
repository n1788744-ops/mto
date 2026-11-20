const {
  initializeTestEnvironment,
  assertFails,
  assertSucceeds,
} = require("@firebase/rules-unit-testing");
const { test, before, after, beforeEach } = require("node:test");
const fs = require("node:fs");

let testEnv;
const PROJECT_ID = process.env.GCP_PROJECT || "demo-no-project";
const ALICE_UID = "alice_123";
const BOB_UID = "bob_456";

const [emulatorHost, emulatorPortStr] = (process.env.FIRESTORE_EMULATOR_HOST || "127.0.0.1:8085").split(":");
const emulatorPort = parseInt(emulatorPortStr, 10);

before(async () => {
  const rules = fs.readFileSync("./firestore.rules", "utf8");
  testEnv = await initializeTestEnvironment({
    projectId: PROJECT_ID,
    firestore: {
      rules,
      host: emulatorHost,
      port: emulatorPort,
    },
  });
});

after(async () => {
  if (testEnv) {
    await testEnv.cleanup();
  }
});

beforeEach(async () => {
  if (testEnv) {
    await testEnv.clearFirestore();
  }
});

test("Unauthenticated user cannot read or write user profiles, favorites, or bookings", async () => {
  const unauthDb = testEnv.unauthenticatedContext().firestore();
  const now = new Date(Date.now() - 1000);

  await assertFails(unauthDb.collection("users").doc(ALICE_UID).get());
  await assertFails(
    unauthDb.collection("users").doc(ALICE_UID).set({
      userId: ALICE_UID,
      fullName: "Алиса",
      phoneNumber: "+79991234567",
      preferredRegion: "melitopol",
      createdAt: now,
      updatedAt: now,
    })
  );
  await assertFails(unauthDb.collection("users").doc(ALICE_UID).collection("favorites").get());
  await assertFails(unauthDb.collection("users").doc(ALICE_UID).collection("bookings").get());
});

test("Owner can register profile and update phoneNumber/preferredRegion without mutating createdAt", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const created = new Date(Date.now() - 10000);

  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).set({
      userId: ALICE_UID,
      fullName: "Алиса Иванова",
      phoneNumber: "+7 990 111 22 33",
      preferredRegion: "melitopol",
      createdAt: created,
      updatedAt: created,
    })
  );

  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).update({
      phoneNumber: "+7 990 444 55 66",
      preferredRegion: "kherson",
      updatedAt: new Date(Date.now() - 1000),
    })
  );

  // Reject ghost field or createdAt tamper
  await assertFails(
    aliceDb.collection("users").doc(ALICE_UID).update({
      createdAt: new Date(Date.now() - 500),
      updatedAt: new Date(Date.now() - 500),
    })
  );
  await assertFails(
    aliceDb.collection("users").doc(ALICE_UID).update({
      isAdmin: true,
      updatedAt: new Date(Date.now() - 500),
    })
  );
});

test("Cross-user isolation blocks Bob from Alice's profile, favorites, and bookings", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();
  const created = new Date(Date.now() - 5000);

  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).set({
      userId: ALICE_UID,
      fullName: "Алиса",
      phoneNumber: "+7 990 111 22 33",
      preferredRegion: "all",
      createdAt: created,
      updatedAt: created,
    })
  );

  await assertFails(bobDb.collection("users").doc(ALICE_UID).get());
  await assertFails(bobDb.collection("users").doc(ALICE_UID).collection("favorites").get());
  await assertFails(bobDb.collection("users").doc(ALICE_UID).collection("bookings").get());
});

test("Owner can manage favorites and YooMoney bookings with terminal state lock", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const created = new Date(Date.now() - 5000);

  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).collection("favorites").doc("apt_mel_1").set({
      userId: ALICE_UID,
      apartmentId: "apt_mel_1",
      apartmentTitle: "2-комн. на пр. Богдана Хмельницкого",
      region: "melitopol",
      dealType: "rent",
      priceRub: 28000,
      createdAt: created,
      updatedAt: created,
    })
  );
  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).collection("favorites").get()
  );

  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).collection("bookings").doc("bk_1").set({
      userId: ALICE_UID,
      apartmentId: "apt_mel_1",
      apartmentTitle: "2-комн. на пр. Богдана Хмельницкого",
      dealType: "rent",
      amountRub: 28000,
      yoomoneyWallet: "4100119599343199",
      status: "pending_payment",
      comment: "Залог за первый месяц",
      createdAt: created,
      updatedAt: created,
    })
  );

  // Reject wrong YooMoney wallet
  await assertFails(
    aliceDb.collection("users").doc(ALICE_UID).collection("bookings").doc("bk_bad").set({
      userId: ALICE_UID,
      apartmentId: "apt_mel_1",
      apartmentTitle: "2-комн.",
      dealType: "rent",
      amountRub: 28000,
      yoomoneyWallet: "9999999999999999",
      status: "pending_payment",
      createdAt: created,
      updatedAt: created,
    })
  );

  // Confirm booking and verify terminal state lock prevents later mutation
  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).collection("bookings").doc("bk_1").update({
      status: "confirmed",
      updatedAt: new Date(Date.now() - 1000),
    })
  );
  await assertFails(
    aliceDb.collection("users").doc(ALICE_UID).collection("bookings").doc("bk_1").update({
      status: "pending_payment",
      updatedAt: new Date(Date.now() - 500),
    })
  );
});
