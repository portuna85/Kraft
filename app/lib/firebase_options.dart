// ⚠️  이 파일은 플레이스홀더입니다.
//    실제 Firebase 프로젝트를 연결하려면 아래 중 하나를 실행하세요:
//
//    1. FlutterFire CLI (권장):
//       dart pub global activate flutterfire_cli
//       flutterfire configure
//
//    2. Firebase Console에서 직접 값 복사:
//       https://console.firebase.google.com → 프로젝트 설정 → 앱 추가
//       발급된 값으로 아래 apiKey, appId, messagingSenderId, projectId 를 교체

import 'package:firebase_core/firebase_core.dart' show FirebaseOptions;
import 'package:flutter/foundation.dart' show defaultTargetPlatform, TargetPlatform;

class DefaultFirebaseOptions {
  static FirebaseOptions get currentPlatform {
    switch (defaultTargetPlatform) {
      case TargetPlatform.android:
        return android;
      case TargetPlatform.iOS:
        return ios;
      default:
        throw UnsupportedError(
          'DefaultFirebaseOptions are not supported for this platform.',
        );
    }
  }

  // TODO: Firebase Console에서 발급받은 실제 값으로 교체하세요.
  static const FirebaseOptions android = FirebaseOptions(
    apiKey: 'REPLACE_WITH_REAL_ANDROID_API_KEY',
    appId: '1:000000000000:android:0000000000000000000000',
    messagingSenderId: '000000000000',
    projectId: 'kraft-lotto-placeholder',
    storageBucket: 'kraft-lotto-placeholder.firebasestorage.app',
  );

  // TODO: Firebase Console에서 발급받은 실제 값으로 교체하세요.
  static const FirebaseOptions ios = FirebaseOptions(
    apiKey: 'REPLACE_WITH_REAL_IOS_API_KEY',
    appId: '1:000000000000:ios:0000000000000000000000',
    messagingSenderId: '000000000000',
    projectId: 'kraft-lotto-placeholder',
    storageBucket: 'kraft-lotto-placeholder.firebasestorage.app',
    iosBundleId: 'com.kraft.kraftLotto',
  );
}
