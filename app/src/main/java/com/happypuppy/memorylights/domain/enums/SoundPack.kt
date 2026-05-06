package com.happypuppy.memorylights.domain.enums

import androidx.annotation.StringRes
import com.happypuppy.memorylights.R

/**
 * Display strings live in `res/values/strings.xml` so they can be translated;
 * resolve via `stringResource(soundPack.displayNameRes)` at the UI layer. The
 * integer resource IDs are platform-agnostic and don't pull a Context
 * dependency into the domain layer.
 */
enum class SoundPack(
    @StringRes val displayNameRes: Int,
    @StringRes val descriptionRes: Int,
    val resourcePrefix: String
) {
    STANDARD(R.string.sound_pack_standard_name, R.string.sound_pack_standard_desc, "standard"),
    FUNNY(R.string.sound_pack_funny_name, R.string.sound_pack_funny_desc, "funny"),
    ELECTRONIC(R.string.sound_pack_electronic_name, R.string.sound_pack_electronic_desc, "electronic"),
    RETRO(R.string.sound_pack_retro_name, R.string.sound_pack_retro_desc, "retro"),
    MUSICAL(R.string.sound_pack_musical_name, R.string.sound_pack_musical_desc, "musical"),
    NATURE(R.string.sound_pack_nature_name, R.string.sound_pack_nature_desc, "nature"),
    SCI_FI(R.string.sound_pack_scifi_name, R.string.sound_pack_scifi_desc, "scifi")
}
