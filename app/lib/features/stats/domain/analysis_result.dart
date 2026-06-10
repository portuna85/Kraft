import 'package:freezed_annotation/freezed_annotation.dart';

part 'analysis_result.freezed.dart';
part 'analysis_result.g.dart';

@freezed
class AnalysisResult with _$AnalysisResult {
  const factory AnalysisResult({
    required List<int> numbers,
    required int firstPrizeCount,
    required int secondPrizeCount,
    required List<PrizeHit> firstPrizeHits,
    required List<PrizeHit> secondPrizeHits,
  }) = _AnalysisResult;

  factory AnalysisResult.fromJson(Map<String, dynamic> json) =>
      _$AnalysisResultFromJson(json);
}

@freezed
class PrizeHit with _$PrizeHit {
  const factory PrizeHit({
    required int round,
    required String drawDate,
  }) = _PrizeHit;

  factory PrizeHit.fromJson(Map<String, dynamic> json) =>
      _$PrizeHitFromJson(json);
}
