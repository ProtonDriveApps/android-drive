package me.proton.core.drive.drivelink.shared.presentation.viewstate

import me.proton.core.drive.base.domain.entity.Permissions
import me.proton.core.presentation.R
import org.junit.Assert.assertEquals
import org.junit.Test

class PermissionsViewStateTest {
    @Test
    fun `happy path`() {
        val selected = PermissionsViewState(
            listOf(
                viewer(true),
                editor(false),
            )
        ).selected
        assertEquals(viewer(true), selected)
    }

    @Test(expected = IllegalStateException::class)
    fun `fails with empty list`() {
        PermissionsViewState(emptyList())
    }

    @Test(expected = IllegalStateException::class)
    fun `fails with no selection`() {
        PermissionsViewState(
            listOf(
                viewer(false),
                editor(false),
            )
        )
    }

    @Test(expected = IllegalStateException::class)
    fun `fails with multiple selection`() {
        PermissionsViewState(
            listOf(
                viewer(true),
                editor(true),
            )
        )
    }

    private fun viewer(selected: Boolean) = PermissionViewState(
        icon = R.drawable.ic_proton_eye,
        label = "Viewer",
        selected = selected,
        permissions = Permissions()
    )

    private fun editor(selected: Boolean) = PermissionViewState(
        icon = R.drawable.ic_proton_pen,
        label = "Editor",
        selected = selected,
        permissions = Permissions()
    )
}
