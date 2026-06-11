import 'package:google_mobile_ads/google_mobile_ads.dart';

import 'ad_config.dart';

class AdService {
  AdService._();

  static InterstitialAd? _interstitialAd;
  static int _recommendCount = 0;
  static const int _interstitialFrequency = 3;

  static Future<void> initialize() async {
    if (!AdConfig.isSupported) return;
    await MobileAds.instance.initialize();
    _loadInterstitial();
  }

  static void _loadInterstitial() {
    InterstitialAd.load(
      adUnitId: AdConfig.interstitialId,
      request: const AdRequest(),
      adLoadCallback: InterstitialAdLoadCallback(
        onAdLoaded: (ad) => _interstitialAd = ad,
        onAdFailedToLoad: (_) => _interstitialAd = null,
      ),
    );
  }

  /// 번호 추천 버튼 클릭 시 호출. N회마다 전면 광고 표시.
  static void onRecommendRequested() {
    if (!AdConfig.isSupported) return;
    _recommendCount++;
    if (_recommendCount % _interstitialFrequency == 0) {
      _showInterstitial();
    }
  }

  static void _showInterstitial() {
    final ad = _interstitialAd;
    if (ad == null) return;
    ad.fullScreenContentCallback = FullScreenContentCallback(
      onAdDismissedFullScreenContent: (_) {
        _interstitialAd = null;
        _loadInterstitial();
      },
      onAdFailedToShowFullScreenContent: (_, __) {
        _interstitialAd = null;
        _loadInterstitial();
      },
    );
    ad.show();
    _interstitialAd = null;
  }
}
