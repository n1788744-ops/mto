package com.example.data

import androidx.annotation.DrawableRes
import com.example.R
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

enum class RegionFilter(
  val code: String,
  val displayName: String,
  val shortName: String,
  val hubLat: Double,
  val hubLon: Double,
  val mapX: Float,
  val mapY: Float,
) {
  ALL(
    code = "all",
    displayName = "Все регионы",
    shortName = "Все",
    hubLat = 46.65,
    hubLon = 35.40,
    mapX = 0.50f,
    mapY = 0.50f,
  ),
  MELITOPOL(
    code = "melitopol",
    displayName = "Мелитополь",
    shortName = "Мелитополь",
    hubLat = 46.8489,
    hubLon = 35.3653,
    mapX = 0.53f,
    mapY = 0.42f,
  ),
  KHERSON(
    code = "kherson",
    displayName = "Херсонская область",
    shortName = "Херсонская обл.",
    hubLat = 46.1767,
    hubLon = 34.8000,
    mapX = 0.24f,
    mapY = 0.68f,
  ),
  ZAPORIZHZHIA(
    code = "zaporizhzhia",
    displayName = "Запорожская область",
    shortName = "Запорожская обл.",
    hubLat = 46.7597,
    hubLon = 36.7987,
    mapX = 0.78f,
    mapY = 0.47f,
  );

  companion object {
    fun fromCode(code: String): RegionFilter =
      entries.firstOrNull { it.code == code } ?: ALL
  }
}

enum class DealType(val code: String, val displayName: String, val priceSuffix: String) {
  RENT("rent", "Аренда", "₽/мес"),
  SALE("sale", "Продажа", "₽"),
}

data class ApartmentPhoto(
  @DrawableRes val resId: Int,
  val caption: String,
)

data class Apartment(
  val id: String,
  val title: String,
  val region: RegionFilter,
  val city: String,
  val district: String,
  val address: String,
  val dealType: DealType,
  val priceRub: Long,
  val depositOrBookingRub: Long,
  val rooms: Int, // 0 = Студия
  val areaSqM: Int,
  val floor: Int,
  val totalFloors: Int,
  val description: String,
  val conditionsSummary: String,
  val features: List<String>,
  val mapX: Float,
  val mapY: Float,
  val latitude: Double,
  val longitude: Double,
  val photos: List<ApartmentPhoto>,
)

fun distanceKm(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
  val r = 6371.0
  val dLat = Math.toRadians(lat2 - lat1)
  val dLon = Math.toRadians(lon2 - lon1)
  val a =
    sin(dLat / 2) * sin(dLat / 2) +
      cos(Math.toRadians(lat1)) * cos(Math.toRadians(lat2)) * sin(dLon / 2) * sin(dLon / 2)
  val c = 2 * atan2(sqrt(a), sqrt(1 - a))
  return r * c
}

fun formatRub(amount: Long): String {
  val raw = amount.toString()
  val sb = StringBuilder()
  var count = 0
  for (i in raw.length - 1 downTo 0) {
    sb.append(raw[i])
    count++
    if (count % 3 == 0 && i != 0) sb.append(' ')
  }
  return sb.reverse().toString()
}

object ApartmentCatalog {
  val listings: List<Apartment> =
    listOf(
      // --- МЕЛИТОПОЛЬ ---
      Apartment(
        id = "apt_mel_center_2k",
        title = "2-комн. квартира в центре Мелитополя",
        region = RegionFilter.MELITOPOL,
        city = "Мелитополь",
        district = "Центр · пр. Богдана Хмельницкого",
        address = "Мелитополь, пр. Богдана Хмельницкого, 44",
        dealType = DealType.RENT,
        priceRub = 28000,
        depositOrBookingRub = 28000,
        rooms = 2,
        areaSqM = 58,
        floor = 4,
        totalFloors = 9,
        description =
          "Светлая двухкомнатная квартира с дизайнерским ремонтом в шаговой доступности от парка Горького. Полностью укомплектована мебелью и бытовой техникой.",
        conditionsSummary =
          "Срок аренды от 3 месяцев, залог 28 000 ₽ (возвращается при выезде), оплата брони и первого месяца через ЮMoney (4100119599343199). Счётчики оплачиваются отдельно.",
        features = listOf("Свежий ремонт", "Автономное отопление", "Кондиционер", "Wi-Fi 100 Мбит/с"),
        mapX = 0.52f,
        mapY = 0.41f,
        latitude = 46.8489,
        longitude = 35.3653,
        photos =
          listOf(
            ApartmentPhoto(R.drawable.photo_apt_living_1, "Уютная гостиная с выходом на балкон"),
            ApartmentPhoto(R.drawable.photo_apt_kitchen, "Просторная кухня-столовая"),
            ApartmentPhoto(R.drawable.photo_apt_bedroom, "Спальня и зона отдыха"),
          ),
      ),
      Apartment(
        id = "apt_mel_pobedy_3k_sale",
        title = "3-комн. семейная квартира на пр. 50-летия Победы",
        region = RegionFilter.MELITOPOL,
        city = "Мелитополь",
        district = "Микрорайон · пр. 50-летия Победы",
        address = "Мелитополь, пр. 50-летия Победы, 31",
        dealType = DealType.SALE,
        priceRub = 5400000,
        depositOrBookingRub = 50000,
        rooms = 3,
        areaSqM = 76,
        floor = 5,
        totalFloors = 9,
        description =
          "Просторная семейная квартира с раздельной планировкой, утеплённой лоджией и видом на зелёный сквер. Рядом школа, детский сад и рынок.",
        conditionsSummary =
          "Полный пакет документов для регистрации права собственности. Возможна бронь объекта задатком 50 000 ₽ через ЮMoney (4100119599343199) и рассрочка на 6 месяцев.",
        features = listOf("Изолированные комнаты", "Две лоджии", "Тёплый пол", "Документы готовы"),
        mapX = 0.56f,
        mapY = 0.37f,
        latitude = 46.8620,
        longitude = 35.3820,
        photos =
          listOf(
            ApartmentPhoto(R.drawable.photo_apt_living_2, "Светлая семейная гостиная 24 м²"),
            ApartmentPhoto(R.drawable.photo_apt_kitchen, "Большая кухня со встроенной техникой"),
            ApartmentPhoto(R.drawable.photo_apt_facade_2, "Современный жилой дом"),
          ),
      ),
      Apartment(
        id = "apt_mel_kiziyar_studio",
        title = "Уютная студия в новом доме на Кизияре",
        region = RegionFilter.MELITOPOL,
        city = "Мелитополь",
        district = "Кизияр · ул. Героев Украины",
        address = "Мелитополь, ул. Героев Украины, 118",
        dealType = DealType.RENT,
        priceRub = 21000,
        depositOrBookingRub = 21000,
        rooms = 0,
        areaSqM = 34,
        floor = 2,
        totalFloors = 5,
        description =
          "Современная квартира-студия в тихом районе Мелитополя. Идеальна для 1–2 человек: ортопедический матрас, смарт-ТВ и тёплая ванна.",
        conditionsSummary =
          "Договор найма от 1 месяца, залог 21 000 ₽ можно разбить на 2 платежа. Онлайн-бронирование через ЮMoney (4100119599343199).",
        features = listOf("Новый ремонт", "Индивидуальный котёл", "Тихий двор", "Быстрое заселение"),
        mapX = 0.49f,
        mapY = 0.45f,
        latitude = 46.8350,
        longitude = 35.3510,
        photos =
          listOf(
            ApartmentPhoto(R.drawable.photo_apt_bedroom, "Уютная зона отдыха в студии"),
            ApartmentPhoto(R.drawable.photo_apt_living_1, "Гостиная с диваном и ТВ"),
            ApartmentPhoto(R.drawable.photo_apt_kitchen, "Оборудованная кухонная зона"),
          ),
      ),
      Apartment(
        id = "apt_mel_noviy_1k_sale",
        title = "1-комн. квартира в ЖК «Южный Квартал»",
        region = RegionFilter.MELITOPOL,
        city = "Мелитополь",
        district = "Новый Мелитополь",
        address = "Мелитополь, ул. Интеркультурная, 154",
        dealType = DealType.SALE,
        priceRub = 3450000,
        depositOrBookingRub = 35000,
        rooms = 1,
        areaSqM = 43,
        floor = 6,
        totalFloors = 10,
        description =
          "Квартира с предчистовой и дизайнерской отделкой в современном жилом комплексе. Закрытый двор без машин, детская площадка.",
        conditionsSummary =
          "Прямая продажа от компании без комиссии. Бронирование цены на 14 дней — 35 000 ₽ через ЮMoney (4100119599343199).",
        features = listOf("Закрытый двор", "Панорамные окна", "Без комиссии", "Лифт OTIS"),
        mapX = 0.47f,
        mapY = 0.39f,
        latitude = 46.8425,
        longitude = 35.3310,
        photos =
          listOf(
            ApartmentPhoto(R.drawable.photo_apt_facade_2, "Фасад ЖК «Южный Квартал»"),
            ApartmentPhoto(R.drawable.photo_apt_living_2, "Светлая жилая комната"),
            ApartmentPhoto(R.drawable.photo_apt_bedroom, "Спальная зона"),
          ),
      ),

      // --- ХЕРСОНСКАЯ ОБЛАСТЬ ---
      Apartment(
        id = "apt_khe_genichesk_2k_rent",
        title = "2-комн. у моря в Геническе с террасой",
        region = RegionFilter.KHERSON,
        city = "Геническ",
        district = "Херсонская обл. · Приморский",
        address = "Геническ, ул. Приморская, 18",
        dealType = DealType.RENT,
        priceRub = 32000,
        depositOrBookingRub = 32000,
        rooms = 2,
        areaSqM = 62,
        floor = 3,
        totalFloors = 5,
        description =
          "Видовая квартира в 5 минутах пешком от Азовского моря и набережной. Просторная терраса для утреннего кофе и собственное парковочное место.",
        conditionsSummary =
          "Долгосрочная аренда и помесячное проживание. Залог 100%, предоплата фиксируется через ЮMoney (4100119599343199).",
        features = listOf("Вид на море", "Своя терраса", "Паркоместо", "Кондиционеры в 2 комнатах"),
        mapX = 0.35f,
        mapY = 0.72f,
        latitude = 46.1767,
        longitude = 34.8000,
        photos =
          listOf(
            ApartmentPhoto(R.drawable.photo_apt_sea_view, "Терраса с панорамным видом на море"),
            ApartmentPhoto(R.drawable.photo_apt_living_1, "Гостиная в тёплых тонах"),
            ApartmentPhoto(R.drawable.photo_apt_kitchen, "Светлая кухня-столовая"),
          ),
      ),
      Apartment(
        id = "apt_khe_skadovsk_1k_sale",
        title = "1-комн. квартира в Скадовске у набережной",
        region = RegionFilter.KHERSON,
        city = "Скадовск",
        district = "Херсонская обл. · Центр",
        address = "Скадовск, ул. Мангубинская, 27",
        dealType = DealType.SALE,
        priceRub = 3150000,
        depositOrBookingRub = 30000,
        rooms = 1,
        areaSqM = 41,
        floor = 2,
        totalFloors = 5,
        description =
          "Тёплая кирпичная квартира рядом с парком и пляжем Джарылгачского залива. Отлично подходит как для жизни, так и для сдачи в аренду.",
        conditionsSummary =
          "Один собственник, юридическое сопровождение сделки включено. Задаток бронирования — 30 000 ₽ на счёт ЮMoney (4100119599343199).",
        features = listOf("Кирпичный дом", "7 мин до пляжа", "Высокий арендный спрос", "Мебель остаётся"),
        mapX = 0.14f,
        mapY = 0.70f,
        latitude = 46.1180,
        longitude = 32.9120,
        photos =
          listOf(
            ApartmentPhoto(R.drawable.photo_apt_living_2, "Светлая гостиная с паркетом"),
            ApartmentPhoto(R.drawable.photo_apt_sea_view, "Выход к морю и набережной"),
            ApartmentPhoto(R.drawable.photo_apt_bedroom, "Спальная комната"),
          ),
      ),
      Apartment(
        id = "apt_khe_novakahovka_2k_sale",
        title = "2-комн. квартира в Новой Каховке",
        region = RegionFilter.KHERSON,
        city = "Новая Каховка",
        district = "Херсонская обл. · Парковая",
        address = "Новая Каховка, пр. Днепровский, 14",
        dealType = DealType.SALE,
        priceRub = 3650000,
        depositOrBookingRub = 40000,
        rooms = 2,
        areaSqM = 56,
        floor = 3,
        totalFloors = 5,
        description =
          "Квартира с капитальным ремонтом, новой электропроводкой и встроенной кухней. Тихий зелёный район рядом с парком и набережной.",
        conditionsSummary =
          "Чистая продажа, быстрый выход на сделку. Бронирование объекта через ЮMoney (4100119599343199).",
        features = listOf("Капитальный ремонт", "Новая проводка", "Стеклопакеты", "Кладовая"),
        mapX = 0.21f,
        mapY = 0.44f,
        latitude = 46.7510,
        longitude = 33.3600,
        photos =
          listOf(
            ApartmentPhoto(R.drawable.photo_apt_kitchen, "Современная кухня"),
            ApartmentPhoto(R.drawable.photo_apt_living_1, "Просторная гостиная"),
            ApartmentPhoto(R.drawable.photo_apt_bedroom, "Уютная спальня"),
          ),
      ),
      Apartment(
        id = "apt_khe_arabat_studio_rent",
        title = "Апартаменты-студия на Арабатской стрелке",
        region = RegionFilter.KHERSON,
        city = "Счастливцево",
        district = "Херсонская обл. · Арабатская стрелка",
        address = "с. Счастливцево, ул. Морская, 9А",
        dealType = DealType.RENT,
        priceRub = 24000,
        depositOrBookingRub = 24000,
        rooms = 0,
        areaSqM = 36,
        floor = 2,
        totalFloors = 3,
        description =
          "Светлые курортные апартаменты с кухней, кондиционером и видом на море. Удобно для круглогодичного проживания или удалённой работы.",
        conditionsSummary =
          "Аренда от 1 месяца, коммунальные услуги фиксированы. Онлайн-оплата бронирования через ЮMoney (4100119599343199).",
        features = listOf("Первая линия", "Стабильный интернет", "Закрытая территория", "Парковка"),
        mapX = 0.38f,
        mapY = 0.80f,
        latitude = 46.0300,
        longitude = 34.8350,
        photos =
          listOf(
            ApartmentPhoto(R.drawable.photo_apt_sea_view, "Морской бриз с террасы"),
            ApartmentPhoto(R.drawable.photo_apt_bedroom, "Интерьер студии"),
            ApartmentPhoto(R.drawable.photo_apt_living_2, "Гостиная зона"),
          ),
      ),

      // --- ЗАПОРОЖСКАЯ ОБЛАСТЬ ---
      Apartment(
        id = "apt_zap_berdyansk_2k_sale",
        title = "2-комн. видовая квартира в Бердянске",
        region = RegionFilter.ZAPORIZHZHIA,
        city = "Бердянск",
        district = "Запорожская обл. · Набережная",
        address = "Бердянск, ул. Горького, 41",
        dealType = DealType.SALE,
        priceRub = 4950000,
        depositOrBookingRub = 50000,
        rooms = 2,
        areaSqM = 64,
        floor = 7,
        totalFloors = 9,
        description =
          "Квартира с панорамным остеклением и прямым видом на Бердянский залив. В пешей доступности Приморская площадь, магазины и кафе.",
        conditionsSummary =
          "Оформление по российскому законодательству с полной проверкой истории. Бронь задатком 50 000 ₽ через ЮMoney (4100119599343199).",
        features = listOf("Вид на залив", "Евроремонт", "Тёплая лоджия", "Лифт"),
        mapX = 0.81f,
        mapY = 0.49f,
        latitude = 46.7597,
        longitude = 36.7987,
        photos =
          listOf(
            ApartmentPhoto(R.drawable.photo_apt_sea_view, "Видовая терраса у моря"),
            ApartmentPhoto(R.drawable.photo_apt_living_1, "Гостиная с мебелью"),
            ApartmentPhoto(R.drawable.photo_apt_kitchen, "Оборудованная кухня"),
          ),
      ),
      Apartment(
        id = "apt_zap_berdyansk_1k_rent",
        title = "1-комн. квартира в центре Бердянска",
        region = RegionFilter.ZAPORIZHZHIA,
        city = "Бердянск",
        district = "Запорожская обл. · Азмол / Центр",
        address = "Бердянск, пр. Азовский, 22",
        dealType = DealType.RENT,
        priceRub = 25000,
        depositOrBookingRub = 25000,
        rooms = 1,
        areaSqM = 42,
        floor = 4,
        totalFloors = 5,
        description =
          "Ухоженная однокомнатная квартира на центральном проспекте. Вся инфраструктура рядом: остановки, аптеки, супермаркет, море в 10 минутах.",
        conditionsSummary =
          "Сдаётся по договору на срок от 2 месяцев. Залог 25 000 ₽. Оплата доступна через ЮMoney (4100119599343199).",
        features = listOf("Центр города", "Стиральная машина", "Бойлер", "Балкон"),
        mapX = 0.77f,
        mapY = 0.45f,
        latitude = 46.7680,
        longitude = 36.7850,
        photos =
          listOf(
            ApartmentPhoto(R.drawable.photo_apt_living_2, "Светлая жилая комната"),
            ApartmentPhoto(R.drawable.photo_apt_bedroom, "Зона отдыха"),
            ApartmentPhoto(R.drawable.photo_apt_facade_2, "Современный жилой комплекс"),
          ),
      ),
      Apartment(
        id = "apt_zap_primorsk_3k_sale",
        title = "3-комн. квартира в Приморске с ремонтом",
        region = RegionFilter.ZAPORIZHZHIA,
        city = "Приморск",
        district = "Запорожская обл. · Центральный",
        address = "Приморск, ул. Морская, 56",
        dealType = DealType.SALE,
        priceRub = 4200000,
        depositOrBookingRub = 40000,
        rooms = 3,
        areaSqM = 72,
        floor = 3,
        totalFloors = 5,
        description =
          "Большая квартира для семьи в курортном Приморске. Раздельный санузел, просторная кухня 11 м², тихий зелёный двор.",
        conditionsSummary =
          "Полная стоимость в договоре, возможна рассрочка первого взноса. Задаток через ЮMoney (4100119599343199).",
        features = listOf("Для большой семьи", "Кухня 11 м²", "Индивидуальное отопление", "Гараж рядом"),
        mapX = 0.68f,
        mapY = 0.51f,
        latitude = 46.7270,
        longitude = 36.3510,
        photos =
          listOf(
            ApartmentPhoto(R.drawable.photo_apt_kitchen, "Просторная кухня-столовая"),
            ApartmentPhoto(R.drawable.photo_apt_living_1, "Семейная гостиная"),
            ApartmentPhoto(R.drawable.photo_apt_sea_view, "Вид с террасы"),
          ),
      ),
      Apartment(
        id = "apt_zap_energodar_2k_rent",
        title = "2-комн. квартира в Энергодаре",
        region = RegionFilter.ZAPORIZHZHIA,
        city = "Энергодар",
        district = "Запорожская обл. · 4-й микрорайон",
        address = "Энергодар, пр. Строителей, 29",
        dealType = DealType.RENT,
        priceRub = 23000,
        depositOrBookingRub = 23000,
        rooms = 2,
        areaSqM = 54,
        floor = 6,
        totalFloors = 9,
        description =
          "Чистая и тёплая двухкомнатная квартира в современном микрорайоне. Есть вся необходимая мебель, холодильник, микроволновка и интернет.",
        conditionsSummary =
          "Длительная аренда, официальный договор с отчётными документами. Оплата аренды и залога через ЮMoney (4100119599343199).",
        features = listOf("Отчётные документы", "Тёплый дом", "Новый лифт", "Быстрый выезд"),
        mapX = 0.44f,
        mapY = 0.23f,
        latitude = 47.4989,
        longitude = 34.6558,
        photos =
          listOf(
            ApartmentPhoto(R.drawable.photo_apt_living_1, "Светлая гостиная"),
            ApartmentPhoto(R.drawable.photo_apt_living_2, "Вторая комната"),
            ApartmentPhoto(R.drawable.photo_apt_facade_2, "Многоэтажный дом"),
          ),
      ),
    )
}
