import 'package:freezed_annotation/freezed_annotation.dart';

part 'analysis_result.freezed.dart';
part 'analysis_result.g.dart';

@freezed
class AnalysisResult with _$AnalysisResult {
  const factory AnalysisResult({
    required List<int> numbers,
    required int matchCount1st,
    required int matchCount2nd,
    required int matchCount3rd,
    required int matchCount4th,
    required int matchCount5th,
    required int totalRoundsChecked,
  }) = _AnalysisResult;

  factory AnalysisResult.fromJson(Map<String, dynamic> json) =>
      _$AnalysisResultFromJson(json);
}
