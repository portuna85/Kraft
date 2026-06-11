import 'dart:io';

import 'package:home_widget/home_widget.dart';

import '../api/api_client.dart';
import '../../features/rounds/domain/round.dart';

class HomeWidgetService {
  static const _androidProvider = 'LottoWidgetProvider';

  static Future<void> update(KraftApiClient client) async {
    if (!Platform.isAndroid && !Platform.isIOS) return;
    try {
      final res = await client.getLatestRound();
      final round = res.data;
      if (round != null) await _saveAndNotify(round);
    } catch (_) {
      // 위젯 갱신 실패는 앱 동작에 영향을 주지 않는다
    }
  }

  static Future<void> _saveAndNotify(Round round) async {
    final numbersStr = round.numbers.join(',');
    final dday = _calcDday();

    await Future.wait([
      HomeWidget.saveWidgetData<String>('widget_round', '${round.round}회'),
      HomeWidget.saveWidgetData<String>('widget_date', round.drawDate),
      HomeWidget.saveWidgetData<String>('widget_numbers', numbersStr),
      HomeWidget.saveWidgetData<String>('widget_bonus', round.bonusNumber.toString()),
      HomeWidget.saveWidgetData<String>('widget_dday', dday),
    ]);

    await HomeWidget.updateWidget(androidName: _androidProvider);
  }

  static String _calcDday() {
    final now = DateTime.now();
    var next = now;
    // 매주 토요일 추첨
    while (next.weekday != DateTime.saturday) {
      next = next.add(const Duration(days: 1));
    }
    final days = next.difference(DateTime(now.year, now.month, now.day)).inDays;
    if (days == 0) return 'D-day';
    return 'D-$days';
  }
}
