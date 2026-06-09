@ui @smoke
Feature: SauceDemo login page
  Hello-world UI test: launch a browser, load the login page, and verify it
  rendered. Proves the UI plumbing end to end — driver creation, page object,
  explicit waits, hard assertions, and the report with screenshot-on-failure.

  @P1
  Scenario: Login page loads successfully
    Given I open the SauceDemo login page
    Then the login form should be displayed
    And the page title should be "Swag Labs"
