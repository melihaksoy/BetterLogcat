package com.melihaksoy.betterlogcat.model

/**
 * What a preset rule colors, shared by every library preset. The color belongs to the role, so a
 * change to "Status 2xx" applies to each preset that has it. [id] is saved in settings; never change it.
 */
enum class HighlightRole(
    val id: String,
    val labelKey: String,
    val defaultArgb: Long,
) {
    HTTP_SECTION(
        id = "http.section",
        labelKey = "colors.role.http.section",
        defaultArgb = 0xFFC678DD,
    ),
    HTTP_METHOD(
        id = "http.method",
        labelKey = "colors.role.http.method",
        defaultArgb = 0xFF56B6C2,
    ),
    HTTP_HEADER(
        id = "http.header",
        labelKey = "colors.role.http.header",
        defaultArgb = 0xFF7C83FD,
    ),
    HTTP_INFO(
        id = "http.info",
        labelKey = "colors.role.http.info",
        defaultArgb = 0xFF61AFEF,
    ),
    HTTP_SUCCESS(
        id = "http.success",
        labelKey = "colors.role.http.success",
        defaultArgb = 0xFF2EB67D,
    ),
    HTTP_CLIENT_ERROR(
        id = "http.clientError",
        labelKey = "colors.role.http.clientError",
        defaultArgb = 0xFFF39C12,
    ),
    HTTP_SERVER_ERROR(
        id = "http.serverError",
        labelKey = "colors.role.http.serverError",
        defaultArgb = 0xFFE06C75,
    ),
    HTTP_FAILED(
        id = "http.failed",
        labelKey = "colors.role.http.failed",
        defaultArgb = 0xFFE06C75,
    ),
    ;

    companion object {
        fun fromId(id: String): HighlightRole? = entries.firstOrNull { it.id == id }
    }
}
