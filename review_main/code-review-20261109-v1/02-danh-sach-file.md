# Các file đề xuất thay đổi

Source gốc chưa thay đổi. Thư mục files chứa nguyên văn code đề xuất.

```text
 database/cinesnack.sql                             |  47 ++++++
 database/theater-review-migration.sql              |  61 +++++++
 .../account/config/ApiSecurityConfig.java          |   1 +
 .../repository/TicketCheckInRepository.java        |   9 +-
 .../booking/service/TicketCheckInService.java      |  10 +-
 .../catalog/controller/AdminCatalogController.java |  11 ++
 .../controller/TheaterPolicyController.java        |  27 ++++
 .../com/cinesnack/catalog/dto/AdminCatalog.java    |  37 ++++-
 .../java/com/cinesnack/catalog/dto/Catalog.java    |  18 ++-
 .../com/cinesnack/catalog/dto/TheaterPolicy.java   |  32 ++++
 .../catalog/repository/AdminCatalogRepository.java |  33 +++-
 .../catalog/repository/CatalogRepository.java      |  11 +-
 .../repository/TheaterPolicyRepository.java        |  39 +++++
 .../catalog/service/AdminCatalogService.java       |  33 +++-
 .../catalog/service/MoviePosterService.java        |   4 +-
 .../catalog/service/TheaterPolicyService.java      |  54 +++++++
 .../payment/service/QrPaymentService.java          |  14 +-
 .../resources/UI/assets/css/showtime-timeline.css  |  22 +++
 src/main/resources/UI/assets/css/ticket-print.css  |  14 ++
 src/main/resources/UI/assets/js/admin.js           |  17 +-
 src/main/resources/UI/assets/js/checkout.js        |  63 +++++++-
 src/main/resources/UI/assets/js/movies.js          |  13 ++
 .../resources/UI/assets/js/order-payment-qr.js     |  98 ++++++++++++
 src/main/resources/UI/assets/js/orders.js          | 130 +++++----------
 .../resources/UI/assets/js/payment-deadline.js     |  37 +++++
 src/main/resources/UI/assets/js/show-pricing.js    |  21 +++
 .../resources/UI/assets/js/showtime-timeline.js    | 178 +++++++++++++++++++++
 .../resources/UI/assets/js/theater-settings.js     |  74 +++++++++
 src/main/resources/UI/assets/js/theater.js         |   8 +-
 src/main/resources/UI/assets/js/ticket-code.js     |  16 ++
 src/main/resources/UI/assets/js/ticket-print.js    |  11 +-
 src/main/resources/UI/html/admin.html              |  18 ++-
 src/main/resources/UI/html/checkout.html           |   2 +
 src/main/resources/UI/html/orders.html             |   4 +
 src/main/resources/UI/html/theater.html            |  29 ++++
 .../com/cinesnack/controller/ApiSecurityTest.java  |  30 ++++
 .../repository/CatalogRepositoryTest.java          |  14 +-
 .../repository/ShowtimeTimelineRepositoryTest.java |  35 ++++
 .../repository/TheaterPolicyRepositoryTest.java    |  42 +++++
 .../repository/TicketCheckInRepositoryTest.java    |  34 ++--
 .../cinesnack/service/AdminCatalogServiceTest.java |  24 ++-
 .../service/MovieMetadataTimelineTest.java         |  71 ++++++++
 .../cinesnack/service/MoviePosterServiceTest.java  |   8 +-
 .../cinesnack/service/QrPaymentServiceTest.java    |   2 +
 .../com/cinesnack/service/SqlServerPaymentIT.java  |   2 +
 .../service/TheaterPolicyServiceTest.java          |  45 ++++++
 .../service/TicketCheckInServiceTest.java          |  17 +-
 src/test/js/showtime-timeline.test.cjs             |  25 +++
 tests/frontend/browser/movie-timeline.test.cjs     |  77 +++++++++
 tests/frontend/browser/payment-deadline.test.cjs   | 164 +++++++++++++++++++
 tests/frontend/browser/theater-policy.test.cjs     | 130 +++++++++++++++
 tests/frontend/browser/ticket-camera.test.cjs      |   5 +-
 tests/frontend/browser/ticket-qr.test.cjs          |  28 ++++
 tests/frontend/unit/payment-deadline.test.cjs      |  37 +++++
 54 files changed, 1839 insertions(+), 147 deletions(-)
```
