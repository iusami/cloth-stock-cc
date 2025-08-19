# Design Document - Gallery Item Deletion Feature

## Overview

The Gallery Item Deletion feature provides a comprehensive solution for users to safely delete clothing items from their collection through an intuitive selection-based interface. This feature integrates seamlessly with the existing MVVM architecture and leverages the current gallery infrastructure while adding robust deletion capabilities with proper error handling, confirmation dialogs, and file system cleanup.

## Architecture

### MVVM Architecture Integration

The deletion feature extends the existing MVVM pattern without disrupting current functionality:

```
┌─────────────────┐    ┌─────────────────┐    ┌─────────────────┐
│   View Layer    │    │  ViewModel      │    │   Model Layer   │
│                 │    │                 │    │                 │
│ • GalleryFragment│◄──►│ • GalleryViewModel│◄──►│ • ClothRepository│
│   (Selection)   │    │   (Deletion)    │    │ • ClothDao      │
│ • ClothItemAdapter│   │ • SelectionState│    │ • FileUtils     │
│   (Multi-select)│    │                 │    │                 │
└─────────────────┘    └─────────────────┘    └─────────────────┘
```

### Component Interaction Flow

```mermaid
sequenceDiagram
    participant User as ユーザー
    participant Gallery as ギャラリー画面
    participant Adapter as ClothItemAdapter
    participant ViewModel as GalleryViewModel
    participant Repository as ClothRepository
    participant Dao as ClothDao
    participant FileSystem as ファイルシステム

    User->>Gallery: アイテムを長押し
    Gallery->>Gallery: 選択モードに入る
    Gallery->>Adapter: 選択状態を更新
    Adapter->>Adapter: アイテムをハイライト
    User->>Gallery: 追加アイテムを選択
    Gallery->>Adapter: 複数選択状態を更新
    User->>Gallery: 削除ボタンをタップ
    Gallery->>Gallery: 確認ダイアログを表示
    User->>Gallery: 削除を確認
    Gallery->>ViewModel: deleteSelectedItems()
    ViewModel->>Repository: deleteItems(selectedIds)
    Repository->>Dao: deleteById(id) for each
    Repository->>FileSystem: deleteImageFile(path)
    FileSystem-->>Repository: 削除結果
    Dao-->>Repository: 削除結果
    Repository-->>ViewModel: 削除完了
    ViewModel-->>Gallery: UI更新
    Gallery->>Gallery: 選択モードを終了
    Gallery->>Adapter: データを更新
</sequenceDiagram>
```

## Components and Interfaces

### 1. Selection State Management

#### SelectionState Data Class (New)
```kotlin
data class SelectionState(
    val isSelectionMode: Boolean = false,
    val selectedItemIds: Set<Long> = emptySet(),
    val totalSelectedCount: Int = 0
) {
    fun isItemSelected(itemId: Long): Boolean = selectedItemIds.contains(itemId)
    fun hasSelection(): Boolean = selectedItemIds.isNotEmpty()
    fun selectItem(itemId: Long): SelectionState = copy(
        selectedItemIds = selectedItemIds + itemId,
        totalSelectedCount = selectedItemIds.size + 1
    )
    fun deselectItem(itemId: Long): SelectionState = copy(
        selectedItemIds = selectedItemIds - itemId,
        totalSelectedCount = selectedItemIds.size - 1
    )
    fun clearSelection(): SelectionState = SelectionState()
}
```

### 2. Gallery Module Enhancement

#### GalleryFragment (Enhancement Required)
- **Purpose**: Handle selection mode UI and deletion confirmation
- **Current Implementation**: Basic gallery display with item click handling
- **Required Enhancement**: Add selection mode support and deletion UI
- **Key Methods**:
  - `enterSelectionMode(itemId: Long)`: Enter selection mode with initial item
  - `exitSelectionMode()`: Exit selection mode and clear selections
  - `showDeleteConfirmationDialog()`: Display deletion confirmation
  - `onDeleteConfirmed()`: Handle confirmed deletion
  - `updateSelectionUI()`: Update action bar and selection indicators

#### GalleryViewModel (Enhancement Required)
- **Purpose**: Manage selection state and coordinate deletion operations
- **Current Implementation**: Basic data loading and filtering
- **Required Enhancement**: Add selection state management and deletion logic
- **Key Properties**:
  - `selectionState: LiveData<SelectionState>`: Observable selection state
  - `isDeletionInProgress: LiveData<Boolean>`: Deletion operation status
  - `deletionResult: LiveData<DeletionResult>`: Deletion operation results
- **Key Methods**:
  - `enterSelectionMode(itemId: Long)`: Initialize selection mode
  - `toggleItemSelection(itemId: Long)`: Toggle item selection state
  - `clearSelection()`: Clear all selections and exit selection mode
  - `deleteSelectedItems()`: Execute deletion of selected items
  - `getSelectedItems()`: Get currently selected ClothItem objects

#### ClothItemAdapter (Enhancement Required)
- **Purpose**: Support multi-selection UI with visual feedback
- **Current Implementation**: Basic item display with click handling
- **Required Enhancement**: Add selection mode support with visual indicators
- **Key Components**:
  - Selection overlay with checkboxes
  - Long-press gesture detection
  - Visual feedback for selected state
- **Key Methods**:
  - `setSelectionMode(enabled: Boolean)`: Enable/disable selection mode
  - `updateSelectionState(selectionState: SelectionState)`: Update selection UI
  - `onItemLongClick(item: ClothItem)`: Handle long-press for selection

### 3. Repository Layer Enhancement

#### ClothRepository (Enhancement Required)
- **Purpose**: Coordinate database and file system deletion operations
- **Current Implementation**: Basic CRUD operations exist
- **Required Enhancement**: Add batch deletion with file cleanup
- **Key Methods**:
  - `deleteItems(itemIds: List<Long>): DeletionResult`: Delete multiple items
  - `deleteItemWithFileCleanup(itemId: Long): Boolean`: Delete item and associated file

#### DeletionResult Data Class (New)
```kotlin
data class DeletionResult(
    val totalRequested: Int,
    val successfulDeletions: Int,
    val failedDeletions: Int,
    val failedItems: List<DeletionFailure> = emptyList()
) {
    val isCompleteSuccess: Boolean = failedDeletions == 0
    val isPartialSuccess: Boolean = successfulDeletions > 0 && failedDeletions > 0
    val isCompleteFailure: Boolean = successfulDeletions == 0
}

data class DeletionFailure(
    val itemId: Long,
    val reason: String,
    val exception: Throwable?
)
```

### 4. File System Integration

#### FileUtils (Enhancement Required)
- **Purpose**: Handle image file deletion with proper error handling
- **Current Implementation**: Basic file operations exist
- **Required Enhancement**: Add safe file deletion with validation
- **Key Methods**:
  - `deleteImageFile(imagePath: String): Boolean`: Delete image file safely
  - `validateFileExists(imagePath: String): Boolean`: Check file existence
  - `getFileSize(imagePath: String): Long`: Get file size for cleanup verification

## Data Models

### Selection State Models

The selection state is managed through dedicated data classes that provide immutable state management:

```kotlin
// Selection state for UI management
data class SelectionState(
    val isSelectionMode: Boolean = false,
    val selectedItemIds: Set<Long> = emptySet()
) {
    val totalSelectedCount: Int = selectedItemIds.size
    fun hasSelection(): Boolean = selectedItemIds.isNotEmpty()
}

// Deletion operation results
data class DeletionResult(
    val totalRequested: Int,
    val successfulDeletions: Int,
    val failedDeletions: Int,
    val failedItems: List<DeletionFailure> = emptyList()
)
```

### Database Operations

The existing ClothDao already provides the necessary deletion methods:
- `deleteById(id: Long): Int` - Delete single item by ID
- `delete(clothItem: ClothItem): Int` - Delete item by object reference

No database schema changes are required as the deletion functionality uses existing operations.

## Error Handling

### Deletion Operation Error Handling

#### Database Deletion Errors
- **Foreign key constraints**: Handle cascading deletion requirements
- **Concurrent access**: Manage database locking and transaction conflicts
- **Data integrity**: Ensure referential integrity during batch operations
- **Transaction rollback**: Implement proper rollback for failed batch operations

#### File System Deletion Errors
- **Permission denied**: Handle file system permission issues
- **File not found**: Gracefully handle missing image files
- **Storage full**: Manage storage space issues during cleanup
- **File in use**: Handle files locked by other processes

#### Partial Deletion Scenarios
- **Mixed success/failure**: Report detailed results for batch operations
- **Recovery options**: Provide retry mechanisms for failed deletions
- **User feedback**: Clear communication of partial deletion results
- **Data consistency**: Ensure database and file system remain synchronized

### User Experience Error Handling

#### Selection Mode Errors
- **Invalid selections**: Prevent selection of non-existent items
- **Memory pressure**: Handle large selection sets efficiently
- **UI state corruption**: Recover from invalid selection states
- **Navigation conflicts**: Handle back button and navigation during selection

#### Confirmation Dialog Errors
- **Dialog dismissal**: Handle unexpected dialog closure
- **System interruptions**: Preserve selection state during interruptions
- **Configuration changes**: Maintain state across device rotation
- **Memory recovery**: Restore selection state after memory pressure

## Testing Strategy

### Unit Tests

#### SelectionState Tests
- Selection state transitions and immutability
- Item selection and deselection logic
- Selection count accuracy
- State validation and edge cases

#### GalleryViewModel Deletion Tests
- Selection mode initialization and management
- Batch deletion operation coordination
- Error handling and result reporting
- State persistence during operations

#### Repository Deletion Tests
- Database deletion operations
- File system cleanup coordination
- Transaction management and rollback
- Error propagation and handling

### Integration Tests

#### End-to-End Deletion Flow
- Complete user workflow from selection to deletion
- Database and file system synchronization
- Error recovery and retry mechanisms
- UI state consistency throughout operation

#### Batch Deletion Tests
- Multiple item selection and deletion
- Partial failure handling and reporting
- Performance with large selection sets
- Memory usage during batch operations

### UI Tests (Espresso)

#### Selection Mode UI Tests
- Long-press gesture recognition
- Selection visual feedback
- Action bar updates during selection
- Multi-selection interaction patterns

#### Deletion Confirmation Tests
- Confirmation dialog display and interaction
- Deletion progress indication
- Success/failure message display
- Selection mode exit after completion

#### Error Scenario UI Tests
- Error message display and formatting
- Retry button functionality
- Graceful degradation during failures
- Accessibility support for error states

## Performance Considerations

### Selection Mode Performance

#### Large Dataset Handling
- Efficient selection state management for thousands of items
- Lazy loading compatibility with selection mode
- Memory-conscious selection tracking
- Optimized UI updates during selection changes

#### UI Responsiveness
- Non-blocking selection state updates
- Smooth visual transitions for selection feedback
- Efficient RecyclerView updates during selection
- Background processing for selection validation

### Deletion Operation Performance

#### Batch Deletion Optimization
- Parallel file deletion operations where safe
- Database transaction batching for efficiency
- Progress reporting for long-running operations
- Memory management during large batch deletions

#### File System Operations
- Asynchronous file deletion to prevent UI blocking
- Efficient file existence validation
- Optimized cleanup verification
- Storage space reclamation monitoring

## Security Considerations

### Data Protection
- Secure deletion of sensitive image files
- Prevention of unauthorized deletion operations
- Validation of deletion permissions
- Audit trail for deletion operations (if required)

### File System Security
- Path traversal prevention in file deletion
- Validation of file ownership before deletion
- Secure handling of file system errors
- Protection against malicious file paths

## Accessibility Considerations

### Selection Mode Accessibility
- Screen reader announcements for selection changes
- Accessible selection count reporting
- Keyboard navigation support for selection
- High contrast support for selection indicators

### Deletion Confirmation Accessibility
- Clear confirmation dialog content descriptions
- Accessible progress reporting during deletion
- Screen reader support for error messages
- Alternative input methods for confirmation actions

## Implementation Phases

### Phase 1: Selection State Management
1. Implement SelectionState data class
2. Add selection mode to GalleryViewModel
3. Update GalleryFragment for selection UI
4. Enhance ClothItemAdapter for multi-selection

### Phase 2: Deletion Operations
1. Implement DeletionResult data classes
2. Add batch deletion to Repository layer
3. Integrate file system cleanup
4. Add deletion confirmation dialog

### Phase 3: Error Handling and Polish
1. Implement comprehensive error handling
2. Add progress reporting and user feedback
3. Optimize performance for large datasets
4. Complete accessibility implementation

### Phase 4: Testing and Validation
1. Comprehensive unit test coverage
2. Integration test implementation
3. UI test automation
4. Performance testing and optimization