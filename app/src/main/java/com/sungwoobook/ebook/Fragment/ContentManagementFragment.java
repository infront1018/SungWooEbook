package com.sungwoobook.ebook.Fragment;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.SwitchCompat;
import androidx.fragment.app.Fragment;

import com.bumptech.glide.Glide;
import com.sungwoobook.ebook.MainActivity;
import com.sungwoobook.ebook.R;

public class ContentManagementFragment extends Fragment {

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_content_management, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        if (getActivity() instanceof MainActivity) {
            ((MainActivity) getActivity()).updateTitle("콘텐츠 관리");
        }

        // 캐시 삭제 로직
        view.findViewById(R.id.btnClearCache).setOnClickListener(v -> {
            new Thread(() -> {
                Glide.get(requireContext()).clearDiskCache();
                if (getActivity() != null) {
                    getActivity().runOnUiThread(() -> {
                        Glide.get(requireContext()).clearMemory();
                        Toast.makeText(getContext(), "캐시 데이터가 삭제되었습니다.", Toast.LENGTH_SHORT).show();
                    });
                }
            }).start();
        });

        // Wi-Fi 자동 재생 설정 (임시 저장)
        SwitchCompat switchWifi = view.findViewById(R.id.switchWifiOnly);
        android.content.SharedPreferences prefs = requireContext().getSharedPreferences("Settings", android.content.Context.MODE_PRIVATE);
        switchWifi.setChecked(prefs.getBoolean("wifi_only_autoplay", true));
        
        switchWifi.setOnCheckedChangeListener((buttonView, isChecked) -> {
            prefs.edit().putBoolean("wifi_only_autoplay", isChecked).apply();
            String msg = isChecked ? "Wi-Fi 환경에서만 영상을 자동 재생합니다." : "모든 환경에서 영상을 자동 재생합니다.";
            Toast.makeText(getContext(), msg, Toast.LENGTH_SHORT).show();
        });
    }
}
