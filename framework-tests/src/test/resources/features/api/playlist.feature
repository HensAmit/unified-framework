@api @smoke
Feature: Spotify catalog API
  Demonstrates the framework's API capability against Spotify's read endpoints:
  query parameters, JsonPath assertions, array-size checks, JSON Schema
  validation, and ${variable} chaining within a scenario.

  @P1
  Scenario: Search for artists returns results
    When I send a GET request to "/search?q=Radiohead&type=artist&limit=5"
    Then I assert the response:
      | type      | path                     | expected    |
      | status    |                          | 200         |
      | jsonpath  | $.artists.items[0].name  | is not null |
      | arraySize | $.artists.items          | 5           |

  @P1
  Scenario: Search then fetch the artist by id (chaining)
    When I send a GET request to "/search?q=Radiohead&type=artist&limit=1"
    Then I assert the response:
      | type   | path                   | expected   |
      | status |                        | 200        |
      | save   | $.artists.items[0].id  | artistId   |
    When I send a GET request to "/artists/${artistId}"
    Then I assert the response:
      | type     | path     | expected           |
      | status   |          | 200                |
      | jsonpath | $.id     | ${artistId}        |
      | jsonpath | $.type   | artist             |
      | schema   | artist-schema.json |          |

  @P2
  Scenario: Browse categories includes paging metadata
    When I send a GET request to "/browse/categories?limit=10"
    Then I assert the response:
      | type      | path                   | expected    |
      | status    |                        | 200         |
      | jsonpath  | $.categories.limit     | 10          |
      | jsonpath  | $.categories.items     | is not null |
