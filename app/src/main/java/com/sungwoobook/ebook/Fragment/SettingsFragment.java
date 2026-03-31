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
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.sungwoobook.ebook.MainActivity;
import com.sungwoobook.ebook.R;
import com.sungwoobook.ebook.adapter.SettingsAdapter;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class SettingsFragment extends Fragment {

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_settings, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // 타이틀 및 내비게이션 확정
        if (getActivity() instanceof MainActivity) {
            MainActivity main = (MainActivity) getActivity();
            main.updateTitle("설정");
            main.setNavActive(MainActivity.NavItem.SETTINGS);
        }

        RecyclerView recyclerView = view.findViewById(R.id.recyclerSettings);
        List<String> menuItems = Arrays.asList("앱 소개", "테마 설정", "책장 넘김 스타일", "콘텐츠 관리", "고객지원", "서비스 정책");

        SettingsAdapter adapter = new SettingsAdapter(menuItems, position -> {
            Fragment subFragment = null;
            switch (position) {
                case 0: subFragment = new AppIntroFragment(); break;
                case 1: subFragment = new ThemeSettingsFragment(); break;
                case 2: subFragment = new FlipSettingsFragment(); break;
                case 3: subFragment = new ContentManagementFragment(); break;
                case 4: subFragment = new CustomerCenterFragment(); break;
                case 5: subFragment = new PolicyFragment(); break;
            }

            if (subFragment != null && getActivity() instanceof MainActivity) {
                ((MainActivity) getActivity()).navigateTo(subFragment, true);
            }
        });

        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        recyclerView.setAdapter(adapter);
    }
}
