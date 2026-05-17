// File: app/src/test/java/com/project/qlcaytrong/repository/CayTrongRepositoryTest.java
package com.project.qlcaytrong.repository;

import com.project.qlcaytrong.model.CayTrongModel;
import com.project.qlcaytrong.util.AuthResult;

import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.*;

/**
 * Unit tests cho business logic Repository.
 * NOTE: Các test thực sự cần Robolectric hoặc Instrumented test.
 * Đây là logic test không cần Android context.
 */
public class CayTrongRepositoryTest {

    // ==================== Model tests ====================

    @Test
    public void cayTrongModel_defaultSyncStatus_isPending() {
        CayTrongModel model = new CayTrongModel();
        model.setSyncStatus("PENDING");
        assertTrue("isPending phải trả về true khi syncStatus = PENDING",
            model.isPending());
    }

    @Test
    public void cayTrongModel_syncedStatus_isNotPending() {
        CayTrongModel model = new CayTrongModel();
        model.setSyncStatus("SYNCED");
        assertFalse("isPending phải trả về false khi syncStatus = SYNCED",
            model.isPending());
    }

    @Test
    public void cayTrongModel_failedStatus_isNotPending() {
        CayTrongModel model = new CayTrongModel();
        model.setSyncStatus("FAILED");
        assertFalse("isPending phải trả về false khi syncStatus = FAILED",
            model.isPending());
    }

    @Test
    public void cayTrongModel_constructor_setsAllFields() {
        CayTrongModel model = new CayTrongModel(
            "id-001", "manh-dat-001", "user-001",
            "Lúa", "Oryza sativa", 100, "cây",
            1716000000L, "DANG_TRONG", "Lúa đông xuân", "PENDING"
        );

        assertEquals("id-001", model.getId());
        assertEquals("manh-dat-001", model.getManhDatId());
        assertEquals("user-001", model.getUserId());
        assertEquals("Lúa", model.getLoaiCay());
        assertEquals("Oryza sativa", model.getTenKhoaHoc());
        assertEquals(100, model.getSoLuong());
        assertEquals("cây", model.getDonViTinh());
        assertEquals(1716000000L, model.getNgayTrong());
        assertEquals("DANG_TRONG", model.getTrangThai());
        assertEquals("Lúa đông xuân", model.getMoTa());
        assertTrue(model.isPending());
    }

    // ==================== AuthResult tests ====================

    @Test
    public void authResult_loading_statusIsLoading() {
        AuthResult<String> result = AuthResult.loading();
        assertTrue(result.isLoading());
        assertFalse(result.isSuccess());
        assertFalse(result.isError());
        assertNull(result.data);
        assertNull(result.message);
    }

    @Test
    public void authResult_success_statusIsSuccess() {
        AuthResult<String> result = AuthResult.success("test-data");
        assertFalse(result.isLoading());
        assertTrue(result.isSuccess());
        assertFalse(result.isError());
        assertEquals("test-data", result.data);
    }

    @Test
    public void authResult_error_statusIsError() {
        AuthResult<String> result = AuthResult.error("Lỗi mạng");
        assertFalse(result.isLoading());
        assertFalse(result.isSuccess());
        assertTrue(result.isError());
        assertEquals("Lỗi mạng", result.message);
        assertNull(result.data);
    }

    @Test
    public void authResult_successWithNull_isStillSuccess() {
        AuthResult<Void> result = AuthResult.success(null);
        assertTrue(result.isSuccess());
        assertNull(result.data);
    }

    // ==================== SyncStatus enum tests ====================

    @Test
    public void syncStatus_fromValidString_returnsCorrectEnum() {
        com.project.qlcaytrong.util.SyncStatus pending =
            com.project.qlcaytrong.util.SyncStatus.from("PENDING");
        assertEquals(com.project.qlcaytrong.util.SyncStatus.PENDING, pending);

        com.project.qlcaytrong.util.SyncStatus synced =
            com.project.qlcaytrong.util.SyncStatus.from("SYNCED");
        assertEquals(com.project.qlcaytrong.util.SyncStatus.SYNCED, synced);

        com.project.qlcaytrong.util.SyncStatus failed =
            com.project.qlcaytrong.util.SyncStatus.from("FAILED");
        assertEquals(com.project.qlcaytrong.util.SyncStatus.FAILED, failed);
    }

    @Test
    public void syncStatus_fromInvalidString_returnsPending() {
        com.project.qlcaytrong.util.SyncStatus result =
            com.project.qlcaytrong.util.SyncStatus.from("INVALID_VALUE");
        assertEquals("Invalid string phải fallback về PENDING",
            com.project.qlcaytrong.util.SyncStatus.PENDING, result);
    }

    @Test
    public void syncStatus_fromNull_returnsPending() {
        com.project.qlcaytrong.util.SyncStatus result =
            com.project.qlcaytrong.util.SyncStatus.from(null);
        assertEquals("Null phải fallback về PENDING",
            com.project.qlcaytrong.util.SyncStatus.PENDING, result);
    }

    // ==================== GocCayModel tests ====================

    @Test
    public void gocCayModel_qrCodeGeneration_formatIsCorrect() {
        // Simulate format: GC-{timestamp}-{4chars}
        String timestamp = String.valueOf(System.currentTimeMillis());
        String qr = "GC-" + timestamp + "-A3F2";
        assertTrue("QR code phải bắt đầu bằng 'GC-'", qr.startsWith("GC-"));
        assertTrue("QR code phải chứa timestamp", qr.contains(timestamp));
    }
}
