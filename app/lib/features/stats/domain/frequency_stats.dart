import 'package:freezed_annotation/freezed_annotation.dart';

part 'frequency_stats.freezed.dart';
part 'frequency_stats.g.dart';

@freezed
class BallFrequency with _$BallFrequency {
  const factory BallFrequency({
    required int number,
    required int count,
    required double rate,
  }) = _BallFrequency;

  factory BallFrequency.fromJson(Map<String, dynamic> json) =>
      _$BallFrequencyFromJson(json);
}
