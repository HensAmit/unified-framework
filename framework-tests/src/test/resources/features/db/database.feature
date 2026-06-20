@db
Feature: Database validation (demonstration)
  Demonstrates the DB-validation capability against a local, seeded schema.
  In a real project these queries would validate the application's own database;
  here they run against a local H2 (MySQL mode) schema the framework seeds itself.

  @smoke
  Scenario: Seeded playlist exists with the expected values
    Then the database query "SELECT name, owner, track_count FROM playlists WHERE id = 1" should return:
      | column      | expected     |
      | name        | Coding Focus |
      | owner       | amit         |
      | track_count | 25           |

  @regression
  Scenario: The playlists table holds the seeded rows
    Then the database query "SELECT * FROM playlists" should return 2 rows
