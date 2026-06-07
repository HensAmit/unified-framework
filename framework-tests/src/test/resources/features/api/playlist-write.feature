@api @userAuth @write
Feature: Spotify playlist write operations
  Exercises the user-auth (refresh token) flow together with PayloadService:
  load a JSON template, mutate it, create a playlist, then clean up by
  unfollowing it. Requires a refresh token with playlist-modify scopes.

  @P1
  Scenario: Create a playlist then remove it
    Given I load the payload "payloads/playlist/create-playlist.json"
    And I update the request payload:
      | path          | value                         | type    |
      | $.name        | Framework Test Playlist       | string  |
      | $.description  | Created by automation; safe to delete | string |
      | $.public      | false                         | boolean |
    When I send a POST request to "/users/${userId}/playlists"
    Then I assert the response:
      | type     | path    | expected    |
      | status   |         | 201         |
      | jsonpath | $.name  | Framework Test Playlist |
      | jsonpath | $.id    | is not null |
      | save     | $.id    | playlistId  |
    When I send a DELETE request to "/playlists/${playlistId}/followers"
    Then I assert the response:
      | type   | path | expected |
      | status |      | 200      |

  @P2
  Scenario: Create a playlist and add a track
    Given I load the payload "payloads/playlist/create-playlist.json"
    And I update the request payload:
      | path     | value                     | type   |
      | $.name   | Framework Track Test      | string |
    When I send a POST request to "/users/${userId}/playlists"
    Then I assert the response:
      | type   | path | expected   |
      | status |      | 201        |
      | save   | $.id | playlistId |
    Given I load the payload "payloads/playlist/add-tracks.json"
    And I update the request payload:
      | op  | path    | value                                  | type   |
      | add | $.uris  | spotify:track:4iV5W9uYEdYUVa79Axb7Rh   | string |
    When I send a POST request to "/playlists/${playlistId}/tracks"
    Then I assert the response:
      | type   | path        | expected    |
      | status |             | 201         |
      | jsonpath | $.snapshot_id | is not null |
    When I send a DELETE request to "/playlists/${playlistId}/followers"
    Then I assert the response:
      | type   | path | expected |
      | status |      | 200      |
