package com.happypuppy.memorylights.domain.enums

/**
 * Enum to represent sound pack options
 * Structured to make it easy to add new sound packs in the future
 */
enum class SoundPack(
    val displayName: String,
    val description: String,
    val resourcePrefix: String
) {
    STANDARD(
        displayName = "Standard",
        description = "Classic Simon game sounds",
        resourcePrefix = "standard"
    ),
    FUNNY(
        displayName = "Funny",
        description = "Humorous sound effects",
        resourcePrefix = "funny"
    ),
    ELECTRONIC(
        displayName = "Electronic",
        description = "Modern electronic sounds",
        resourcePrefix = "electronic"
    ),
    RETRO(
        displayName = "Retro Gaming",
        description = "8-bit style game sounds",
        resourcePrefix = "retro"
    ),
    MUSICAL(
        displayName = "Musical",
        description = "Musical instrument tones",
        resourcePrefix = "standard" // Uses standard sounds until musical sounds are added
    ),
    NATURE(
        displayName = "Nature",
        description = "Peaceful sounds from nature",
        resourcePrefix = "standard" // Uses standard sounds until nature sounds are added
    ),
    SCI_FI(
        displayName = "Sci-Fi",
        description = "Futuristic space sounds",
        resourcePrefix = "standard" // Uses standard sounds until sci-fi sounds are added
    )
}