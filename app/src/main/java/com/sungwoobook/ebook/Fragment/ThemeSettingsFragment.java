package com.sungwoobook.ebook.Fragment;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.RadioGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.sungwoobook.ebook.MainActivity;
import com.sungwoobook.ebook.R;

public class ThemeSettingsFragment extends Fragment {

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_theme_settings, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        if (getActivity() instanceof MainActivity) {
            ((MainActivity) getActivity()).updateTitle("테마 설정");
        }

        RadioGroup radioGroupTheme = view.findViewById(R.id.radioGroupTheme);
        
        // 현재 설정된 테마 불러오기 및 초기 체크 상태 설정
        int currentTheme = com.sungwoobook.ebook.model.ThemeManager.getTheme(requireContext());
        if (currentTheme == com.sungwoobook.ebook.model.ThemeManager.THEME_DARK) {
            radioGroupTheme.check(R.id.radioDark);
        } else if (currentTheme == com.sungwoobook.ebook.model.ThemeManager.THEME_WHITE) {
            radioGroupTheme.check(R.id.radioWhite);
        } else {
            radioGroupTheme.check(R.id.radioDefault);
        }

        radioGroupTheme.setOnCheckedChangeListener((group, checkedId) -> {
            int newTheme;
            String themeName;
            
            if (checkedId == R.id.radioDark) {
                newTheme = com.sungwoobook.ebook.model.ThemeManager.THEME_DARK;
                themeName = "다크 모드";
            } else if (checkedId == R.id.radioWhite) {
                newTheme = com.sungwoobook.ebook.model.ThemeManager.THEME_WHITE;
                themeName = "화이트 모드";
            } else {
                newTheme = com.sungwoobook.ebook.model.ThemeManager.THEME_DEFAULT;
                themeName = "기본 모드";
            }
            
            if (newTheme != currentTheme) {
                com.sungwoobook.ebook.model.ThemeManager.setTheme(requireContext(), newTheme);
                Toast.makeText(getContext(), themeName + "로 변경되었습니다.", Toast.LENGTH_SHORT).show();
                
                // 테마 적용을 위해 액티비티 재시작
                if (getActivity() != null) {
                    getActivity().recreate();
                }
            }
        });
    }
}
