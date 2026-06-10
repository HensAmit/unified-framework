@ui @regression
Feature: SauceDemo end-to-end purchase
  Drives the full shopping flow — login, add to cart, checkout, confirmation —
  with data supplied from an Excel sheet. The Scenario Outline runs once per
  testcase_id; each row's data (credentials, product, checkout details) is read
  from testdata/ui-testdata.xlsx.

  @P1
  Scenario Outline: Complete a purchase end to end
    Given I load test data for "<testcase_id>"
    And I open the SauceDemo login page
    When I log in with the loaded credentials
    Then the inventory page should be displayed
    When I add the test-data product to the cart
    Then the cart badge should show 1 item
    When I open the cart
    Then the cart should contain the test-data product
    When I proceed to checkout
    And I enter the checkout information from test data
    And I finish the checkout
    Then the order confirmation should be displayed

    Examples:
      | testcase_id |
      | TC001       |
      | TC002       |
