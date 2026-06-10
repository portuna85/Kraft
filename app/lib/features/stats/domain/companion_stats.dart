import 'package:freezed_annotation/freezed_annotation.dart';

part 'companion_stats.freezed.dart';
part 'companion_stats.g.dart';

@freezed
class CompanionEntry with _$CompanionEntry {
  const factory CompanionEntry({
    required int number,
    required int count,
    required double percent,
    required int rank,
  }) = _CompanionEntry;

  factory CompanionEntry.fromJson(Map<String, dynamic> json) =>
      _$CompanionEntryFromJson(json);
}
