# Implementation Plan

- [ ] 1. Enhance TagData model to support 10-unit size increments
  - Add SIZE_INCREMENT constant (value: 10) to TagData companion object
  - Implement getValidSizeOptions() method to generate [60, 70, 80, ..., 160] list
  - Update size validation to only accept values from valid options list
  - Write unit tests for new size increment validation logic
  - _Requirements: 3.1, 5.2_

- [ ] 2. Update TaggingActivity size picker configuration for 10-unit increments
  - Modify setupNumberPicker() method to use TagData.getValidSizeOptions()
  - Configure NumberPicker to display only valid size values in 10-unit steps
  - Update size picker value change listener to work with increment-based values
  - Test size picker behavior with new increment constraints
  - _Requirements: 3.1, 5.2_

- [ ] 3. Implement DetailActivity edit button functionality
  - Update navigateToTaggingActivity() method to launch TaggingActivity in edit mode
  - Pass EXTRA_EDIT_MODE=true and EXTRA_CLOTH_ITEM_ID to TaggingActivity intent
  - Remove placeholder Snackbar message and implement actual navigation
  - Add proper error handling for invalid cloth item IDs
  - _Requirements: 2.1, 2.2_

- [ ] 4. Enhance TaggingActivity to handle edit mode initialization
  - Update setupEditMode() method to properly load existing cloth item data
  - Ensure image display works correctly for edit mode (using existing image path)
  - Verify form pre-population with existing tag data works as expected
  - Test edit mode title and UI state changes
  - _Requirements: 2.2, 2.3, 5.1_

- [ ] 5. Test and verify complete gallery-to-edit workflow
  - Write integration tests for complete user flow: Gallery → Detail → Edit → Save
  - Test data persistence after edit operations
  - Verify gallery display updates with modified tag information
  - Test error scenarios and recovery mechanisms
  - _Requirements: 1.1, 2.1, 4.1, 4.4_

- [ ] 6. Add comprehensive unit tests for edit functionality
  - Write tests for TagData.getValidSizeOptions() method
  - Test size validation with 10-unit increment constraints
  - Test TaggingViewModel edit mode behavior and data loading
  - Test DetailActivity navigation to edit mode
  - _Requirements: All requirements verification_