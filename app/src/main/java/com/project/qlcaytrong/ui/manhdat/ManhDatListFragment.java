// File: app/src/main/java/com/project/qlcaytrong/ui/manhdat/ManhDatListFragment.java
package com.project.qlcaytrong.ui.manhdat;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

/**
 * ManhDatListFragment — Fragment wrapper mỏng cho BottomNav Tab "Mảnh đất".
 *
 * Do ManhDatListActivity là Activity (không phải Fragment), Fragment này
 * chỉ làm nhiệm vụ khởi động Activity đó khi tab được chọn.
 *
 * FIX: nav_graph.xml khai báo class này (không tồn tại trước đó),
 *      khiến app crash khi ấn tab Mảnh đất.
 */
public class ManhDatListFragment extends Fragment {

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        // Fragment không render UI riêng — trả về view trống
        return new View(requireContext());
    }

    @Override
    public void onResume() {
        super.onResume();
        // Mỗi khi tab này được hiển thị → mở Activity, không finish Fragment
        Intent intent = new Intent(requireActivity(), ManhDatListActivity.class);
        startActivity(intent);
    }
}
