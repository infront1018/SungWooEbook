package com.sungwoobook.ebook.Fragment;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.sungwoobook.ebook.Login.LoginActivity;
import com.sungwoobook.ebook.R;

public class MoreFragment extends Fragment {

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_more, container, false);

        Button btnSettings = view.findViewById(R.id.btn_settings);
        Button btnHelp = view.findViewById(R.id.btn_help);
        Button btnLogin = view.findViewById(R.id.btn_login);

        btnSettings.setOnClickListener(v -> {
            // TODO: 설정 화면으로 이동
            requireActivity().getSupportFragmentManager().beginTransaction().remove(this).commit();
        });

        btnHelp.setOnClickListener(v -> {
            // TODO: 고객센터 화면으로 이동
            requireActivity().getSupportFragmentManager().beginTransaction().remove(this).commit();
        });

        btnLogin.setOnClickListener(v -> {
            startActivity(new Intent(getContext(), LoginActivity.class));
            requireActivity().getSupportFragmentManager().beginTransaction().remove(this).commit();
        });

        return view;
    }
}
