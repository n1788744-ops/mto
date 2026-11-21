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

test("Unauthenticated user cannot read or write profiles, bookings, used_operations, or consultant_kb", async () => {
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
  await assertFails(unauthDb.collection("users").doc(ALICE_UID).collection("bookings").get());
  await assertFails(unauthDb.collection("used_operations").doc("op_123456").get());
  await assertFails(unauthDb.collection("consultant_kb").get());
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

  await assertFails(
    aliceDb.collection("users").doc(ALICE_UID).update({
      createdAt: new Date(Date.now() - 500),
      updatedAt: new Date(Date.now() - 500),
    })
  );
});

test("Anti-fraud blocks fake pre-paid booking creation, underpayment, and replay across bookings", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();
  const created = new Date(Date.now() - 5000);

  // Reject creating a booking already marked verified_paid
  await assertFails(
    aliceDb.collection("users").doc(ALICE_UID).collection("bookings").doc("bk_fake").set({
      userId: ALICE_UID,
      apartmentId: "apt_mel_center_2k",
      apartmentTitle: "2-комн. квартира",
      dealType: "rent",
      amountRub: 28000,
      yoomoneyWallet: "4100119599343199",
      paymentLabel: "YD_ALICE_123456",
      paymentMethod: "AC",
      status: "verified_paid",
      verificationAttempts: 0,
      createdAt: created,
      updatedAt: created,
    })
  );

  // Create valid awaiting_payment booking
  const bkRef = aliceDb.collection("users").doc(ALICE_UID).collection("bookings").doc("bk_1");
  await assertSucceeds(
    bkRef.set({
      userId: ALICE_UID,
      apartmentId: "apt_mel_center_2k",
      apartmentTitle: "2-комн. квартира в центре Мелитополя",
      dealType: "rent",
      amountRub: 28000,
      yoomoneyWallet: "4100119599343199",
      paymentLabel: "YD_ALICE_998877",
      paymentMethod: "AC",
      status: "awaiting_payment",
      verificationAttempts: 0,
      createdAt: created,
      updatedAt: created,
    })
  );

  // Reject marking verified_paid WITHOUT atomic used_operations entry
  await assertFails(
    bkRef.update({
      status: "verified_paid",
      yoomoneyOperationId: "715492819201",
      verifiedAmountRub: 28000,
      verifiedAt: new Date(Date.now() - 1000),
      updatedAt: new Date(Date.now() - 1000),
    })
  );

  // Reject underpayment even with batch write
  const underpayBatch = aliceDb.batch();
  const underOpRef = aliceDb.collection("used_operations").doc("op_underpay_1");
  underpayBatch.set(underOpRef, {
    operationId: "op_underpay_1",
    bookingId: "bk_1",
    claimedByUid: ALICE_UID,
    amountRub: 100,
    wallet: "4100119599343199",
    createdAt: new Date(Date.now() - 1000),
  });
  underpayBatch.update(bkRef, {
    status: "verified_paid",
    yoomoneyOperationId: "op_underpay_1",
    verifiedAmountRub: 100,
    verifiedAt: new Date(Date.now() - 1000),
    updatedAt: new Date(Date.now() - 1000),
  });
  await assertFails(underpayBatch.commit());

  // Valid atomic verification succeeds
  const validBatch = aliceDb.batch();
  const validOpRef = aliceDb.collection("used_operations").doc("715492819201");
  validBatch.set(validOpRef, {
    operationId: "715492819201",
    bookingId: "bk_1",
    claimedByUid: ALICE_UID,
    amountRub: 28000,
    wallet: "4100119599343199",
    createdAt: new Date(Date.now() - 1000),
  });
  validBatch.update(bkRef, {
    status: "verified_paid",
    yoomoneyOperationId: "715492819201",
    verifiedAmountRub: 28000,
    verifiedAt: new Date(Date.now() - 1000),
    updatedAt: new Date(Date.now() - 1000),
  });
  await assertSucceeds(validBatch.commit());

  // Terminal lock prevents mutating verified_paid booking
  await assertFails(
    bkRef.update({
      status: "cancelled",
      updatedAt: new Date(Date.now() - 500),
    })
  );

  // Anti-replay: Bob cannot reuse the same operationId 715492819201 for his own booking
  const bobBkRef = bobDb.collection("users").doc(BOB_UID).collection("bookings").doc("bk_bob");
  await assertSucceeds(
    bobBkRef.set({
      userId: BOB_UID,
      apartmentId: "apt_mel_center_2k",
      apartmentTitle: "2-комн.",
      dealType: "rent",
      amountRub: 28000,
      yoomoneyWallet: "4100119599343199",
      paymentLabel: "YD_BOB_112233",
      paymentMethod: "AC",
      status: "awaiting_payment",
      verificationAttempts: 0,
      createdAt: created,
      updatedAt: created,
    })
  );
  const replayBatch = bobDb.batch();
  replayBatch.set(bobDb.collection("used_operations").doc("715492819201"), {
    operationId: "715492819201",
    bookingId: "bk_bob",
    claimedByUid: BOB_UID,
    amountRub: 28000,
    wallet: "4100119599343199",
    createdAt: new Date(Date.now() - 500),
  });
  replayBatch.update(bobBkRef, {
    status: "verified_paid",
    yoomoneyOperationId: "715492819201",
    verifiedAmountRub: 28000,
    verifiedAt: new Date(Date.now() - 500),
    updatedAt: new Date(Date.now() - 500),
  });
  await assertFails(replayBatch.commit());
});

test("Signed-in user can read consultant_kb but cannot write to it", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  await assertSucceeds(aliceDb.collection("consultant_kb").get());
  await assertFails(
    aliceDb.collection("consultant_kb").doc("art_1").set({
      articleId: "art_1",
      category: "rent",
      title: "Test",
      summary: "Test",
      keywords: ["аренда"],
      priority: 1,
    })
  );
});
