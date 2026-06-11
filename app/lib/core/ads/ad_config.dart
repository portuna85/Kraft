import 'dart:io';

class AdConfig {
  AdConfig._();

  // 테스트 ID — 실제 배포 시 dart-define으로 교체
  static const _testBannerId     = 'ca-app-pub-3940256099942544/6300978111';
  static const _testInterstitialId = 'ca-app-pub-3940256099942544/1033173712';
  static const _testNativeId     = 'ca-app-pub-3940256099942544/2247696110';

  static const String bannerId = String.fromEnvironment(
    'ADMOB_BANNER_ID',
    defaultValue: _testBannerId,
  );

  static const String interstitialId = String.fromEnvironment(
    'ADMOB_INTERSTITIAL_ID',
    defaultValue: _testInterstitialId,
  );

  static const String nativeId = String.fromEnvironment(
    'ADMOB_NATIVE_ID',
    defaultValue: _testNativeId,
  );

  static bool get isSupported => Platform.isAndroid || Platform.isIOS;
}
