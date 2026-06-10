import 'package:freezed_annotation/freezed_annotation.dart';

part 'page_result.freezed.dart';
part 'page_result.g.dart';

@freezed
class PageResult<T> with _$PageResult<T> {
  const factory PageResult({
    required List<T> content,
    required int page,
    required int size,
    required int totalElements,
    required int totalPages,
    required bool last,
  }) = _PageResult<T>;

  factory PageResult.fromJson(
    Map<String, dynamic> json,
    T Function(Object?) fromJsonT,
  ) =>
      _$PageResultFromJson(json, fromJsonT);
}
