---
inclusion: fileMatch
fileMatchPattern: "*.kt"
---

# Kotlin Code Style Guide

## General Rules
- Use 4 spaces for indentation
- Max line length: 120 characters
- Use trailing commas in multi-line declarations
- Prefer expression bodies for simple functions

## Naming Conventions
- Classes: PascalCase (e.g., `AppListViewModel`)
- Functions: camelCase (e.g., `loadUserApps`)
- Properties: camelCase (e.g., `isLoading`)
- Constants: SCREAMING_SNAKE_CASE (e.g., `MAX_RETRY_COUNT`)
- Private properties: prefix with underscore for backing fields (e.g., `_uiState`)

## Null Safety
- Avoid `!!` operator, use safe calls `?.` or `?:`
- Use `requireNotNull()` or `checkNotNull()` when null is unexpected
- Prefer `val` over `var`

## Coroutines
- Use `viewModelScope` in ViewModels
- Use `lifecycleScope` in Fragments/Activities
- Use `withContext(Dispatchers.IO)` for IO operations
- Handle exceptions with try-catch or Result<T>

## Flow
- Expose StateFlow for UI state
- Use `asStateFlow()` for immutable exposure
- Collect in `lifecycleScope.launch { }`

## Hilt
- Annotate ViewModels with `@HiltViewModel`
- Annotate Activities/Fragments with `@AndroidEntryPoint`
- Use `@Inject constructor` for dependencies

## Error Handling
```kotlin
// Good
fun doSomething(): Result<Unit> {
    return try {
        // operation
        Result.success(Unit)
    } catch (e: Exception) {
        Timber.e(e, "Operation failed")
        Result.failure(e)
    }
}

// Bad
fun doSomething() {
    try {
        // operation
    } catch (e: Exception) {
        e.printStackTrace() // Don't use this
    }
}
```

## ViewBinding Pattern
```kotlin
private var _binding: FragmentXxxBinding? = null
private val binding get() = _binding

override fun onCreateView(...): View {
    _binding = FragmentXxxBinding.inflate(inflater, container, false)
    return _binding!!.root
}

override fun onDestroyView() {
    super.onDestroyView()
    _binding = null
}
```

## Safe Binding Access
```kotlin
// Good - safe access
val b = binding ?: return
b.textView.text = "Hello"

// Bad - force unwrap
binding!!.textView.text = "Hello"
```
