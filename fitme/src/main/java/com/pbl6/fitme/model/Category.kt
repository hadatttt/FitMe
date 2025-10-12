import java.util.UUID

data class Category(
    val categoryId: UUID,
    val categoryName: String,
    val description: String?,
    val imageUrl: String?,
    val isActive: Boolean?,
    val sortOrder: Int?,
    val parentCategoryId: UUID?,
    val subcategories: List<Category> = emptyList()
)
