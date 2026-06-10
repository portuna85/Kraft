import 'package:flutter/foundation.dart';
import 'package:riverpod_annotation/riverpod_annotation.dart';

part 'app_config.g.dart';

@riverpod
AppConfig appConfig(AppConfigRef ref) => kDebugMode
    ? AppConfig.dev()
    : AppConfig.prod();

class AppConfig {
  const AppConfig({required this.baseUrl, this.apiKey});

  factory AppConfig.dev() => const AppConfig(
        baseUrl: 'http://10.0.2.2:8080', // Android 에뮬레이터 → 로컬호스트
        apiKey: null,
      );

  factory AppConfig.prod() => const AppConfig(
        baseUrl: 'https://www.kraft.io.kr',
        apiKey: null, // 필요 시 flutter_secure_storage로 주입
      );

  final String baseUrl;
  final String? apiKey;
}
