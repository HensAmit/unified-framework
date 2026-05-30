@api @smoke @P1
Feature: Spotify API smoke check
  Verifies that the framework can authenticate against Spotify
  and make a successful authenticated GET request.

  Scenario: Browse categories endpoint returns success
    When I send a GET request to "/browse/categories"
    Then the response status code should be 200
