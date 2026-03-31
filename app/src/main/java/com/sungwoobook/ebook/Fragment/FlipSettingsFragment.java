package com.sungwoobook.ebook.Fragment;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.RadioButton;
import android.widget.RadioGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.sungwoobook.ebook.R;

/**
 * 책장 넘김 스타일 설정을 위한 Fragment.
 */
public class FlipSettingsFragment extends Fragment {

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_flip_settings, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        RadioGroup radioGroupFlip = view.findViewById(R.id.radioGroupFlip);
        RadioButton radioClassic = view.findViewById(R.id.radioClassic);
        RadioButton radioPremium = view.findViewById(R.id.radioPremium);

        // 현재 설정값 읽어오기
        SharedPreferences prefs = requireContext().getSharedPreferences("Settings", Context.MODE_PRIVATE);
        int currentStyle = prefs.getInt("page_turn_style", 0); // 기본값 0 (2.5D)

        if (currentStyle == 1) {
            radioPremium.setChecked(true);
        } else {
            radioClassic.setChecked(true);
        }

        // 라디오 버튼 선택 시 즉시 저장
        radioGroupFlip.setOnCheckedChangeListener((group, checkedId) -> {
            int newStyle = (checkedId == R.id.radioPremium) ? 1 : 0;
            prefs.edit().putInt("page_turn_style", newStyle).apply();
            
            // 토스트 메시지로 확인 피드백 제공 (옵션)
            // android.widget.Toast.makeText(getContext(), "설정이 실시간 반영되었습니다.", android.widget.Toast.LENGTH_SHORT).show();
        });
    }
}
