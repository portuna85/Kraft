import 'package:freezed_annotation/freezed_annotation.dart';

part 'round.freezed.dart';
part 'round.g.dart';

@freezed
class Round with _$Round {
  const factory Round({
    required int drwNo,
    required String drwNoDate,
    required int drwtNo1,
    required int drwtNo2,
    required int drwtNo3,
    required int drwtNo4,
    required int drwtNo5,
    required int drwtNo6,
    required int bnusNo,
    required int firstWinamnt,
    required int firstPrzwnerCo,
    required int totSellamnt,
  }) = _Round;

  factory Round.fromJson(Map<String, dynamic> json) => _$RoundFromJson(json);
}

extension RoundX on Round {
  List<int> get mainNumbers =>
      [drwtNo1, drwtNo2, drwtNo3, drwtNo4, drwtNo5, drwtNo6];
}
