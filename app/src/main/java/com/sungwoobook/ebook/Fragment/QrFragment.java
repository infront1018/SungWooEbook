package com.sungwoobook.ebook.Fragment;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.google.zxing.integration.android.IntentIntegrator;
import com.google.zxing.integration.android.IntentResult;
import com.sungwoobook.ebook.R;

public class QrFragment extends Fragment {

    private ImageButton btnClose;
    private ImageButton btnScanQr;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {

        View view = inflater.inflate(R.layout.fragment_qr, container, false);

        // 버튼 초기화
        btnClose = view.findViewById(R.id.btn_close);
        btnScanQr = view.findViewById(R.id.btn_scan_qr);

        // X 버튼 클릭 시 프래그먼트 종료
        btnClose.setOnClickListener(v -> {
            requireActivity()
                    .getSupportFragmentManager()
                    .beginTransaction()
                    .replace(R.id.fragment_container, new HomeFragment())  // 홈 프래그먼트로 교체
                    .commit();
        });

        // QR 촬영 버튼 클릭 시 ZXing 실행
        btnScanQr.setOnClickListener(v -> {
            IntentIntegrator integrator = IntentIntegrator.forSupportFragment(this);
            integrator.setOrientationLocked(false);
            integrator.setPrompt("QR 코드를 스캔하세요");
            integrator.setBeepEnabled(true);
            integrator.setDesiredBarcodeFormats(IntentIntegrator.QR_CODE);
            integrator.initiateScan();
        });

        return view;
    }

    // QR 스캔 결과 처리
    @Override
    public void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        IntentResult result = IntentIntegrator.parseActivityResult(requestCode, resultCode, data);
        if (result != null && result.getContents() != null) {
            // 성공적으로 스캔됨
            String qrData = result.getContents();
            Toast.makeText(getContext(), "QR 결과: " + qrData, Toast.LENGTH_LONG).show();

            // 필요한 처리 추가 (예: 페이지 이동, DB 조회 등)

        } else {
            // 스캔 취소됨
            Toast.makeText(getContext(), "QR 스캔이 취소되었습니다", Toast.LENGTH_SHORT).show();
        }
    }
}
