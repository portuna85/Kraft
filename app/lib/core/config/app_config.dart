import 'package:flutter/foundation.dart';
import 'package:riverpod_annotation/riverpod_annotation.dart';

part 'app_config.g.dart';

/// --dart-define=BASE_URL=http://192.168.x.x:8080 으로 덮어쓸 수 있다.
/// --dart-define=API_KEY=xxx 로 API 키를 주입할 수 있다.
const _kBaseUrlOverride = String.fromEnvironment('BASE_URL');
const _kApiKeyOverride = String.fromEnvironment('API_KEY');

@riverpod
AppConfig appConfig(AppConfigRef ref) => kDebugMode
    ? AppConfig.local()
    : AppConfig.prod();

class AppConfig {
  const AppConfig({required this.baseUrl, this.apiKey});

  factory AppConfig.local() => AppConfig(
        baseUrl: _kBaseUrlOverride.isNotEmpty
            ? _kBaseUrlOverride
            : 'http://10.0.2.2:8080',
        apiKey: _kApiKeyOverride.isNotEmpty ? _kApiKeyOverride : null,
      );

  factory AppConfig.prod() => AppConfig(
        baseUrl: _kBaseUrlOverride.isNotEmpty
            ? _kBaseUrlOverride
            : 'https://www.kraft.io.kr',
        apiKey: _kApiKeyOverride.isNotEmpty ? _kApiKeyOverride : null,
      );

  final String baseUrl;
  final String? apiKey;
}
