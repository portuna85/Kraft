import 'package:flutter/material.dart';

abstract final class AppTheme {
  // 웹 테마색 #061d3a 기반
  static const _primary = Color(0xFF061D3A);
  static const _accent = Color(0xFF1565C0);
  static const _surface = Color(0xFFF8FAFF);

  static ThemeData get light => ThemeData(
        useMaterial3: true,
        colorScheme: ColorScheme.fromSeed(
          seedColor: _primary,
          primary: _primary,
          secondary: _accent,
          surface: _surface,
        ),
        appBarTheme: const AppBarTheme(
          backgroundColor: _primary,
          foregroundColor: Colors.white,
          elevation: 0,
        ),
        navigationBarTheme: NavigationBarThemeData(
          backgroundColor: Colors.white,
          indicatorColor: _accent.withOpacity(0.15),
          labelTextStyle: WidgetStateProperty.all(
            const TextStyle(fontSize: 11, fontWeight: FontWeight.w500),
          ),
        ),
        cardTheme: CardTheme(
          elevation: 1,
          shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(12)),
        ),
      );

  static ThemeData get dark => ThemeData(
        useMaterial3: true,
        colorScheme: ColorScheme.fromSeed(
          seedColor: _primary,
          brightness: Brightness.dark,
          primary: const Color(0xFF90CAF9),
        ),
      );
}
