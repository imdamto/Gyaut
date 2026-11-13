package com.gyaut.app.common;

import android.view.View;

import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

// 화면이 상태바·내비게이션바·키보드에 가려지지 않게 바깥 여백을 주는 공통 코드
public final class WindowInsetsHelper {

    // root: 화면 맨 바깥 뷰 (binding.getRoot())
    public static void applyTo(View root) {
        ViewCompat.setOnApplyWindowInsetsListener(root, (v, insets) -> {
            Insets bars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            Insets keyboard = insets.getInsets(WindowInsetsCompat.Type.ime());
            // 키보드가 올라오면 키보드 높이만큼, 아니면 내비게이션바 높이만큼 아래를 비움
            v.setPadding(bars.left, bars.top, bars.right, Math.max(bars.bottom, keyboard.bottom));
            return insets;
        });
    }

    private WindowInsetsHelper() {}
}