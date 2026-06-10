import 'package:riverpod_annotation/riverpod_annotation.dart';

import '../../../core/api/api_client.dart';
import '../domain/round.dart';
import '../domain/page_result.dart';

part 'rounds_repository.g.dart';

@riverpod
RoundsRepository roundsRepository(RoundsRepositoryRef ref) =>
    RoundsRepository(ref.watch(kraftApiClientProvider));

class RoundsRepository {
  const RoundsRepository(this._client);
  final KraftApiClient _client;

  Future<Round> getLatest() async {
    final res = await _client.getLatestRound();
    return res.data!;
  }

  Future<Round> getByDrwNo(int drwNo) async {
    final res = await _client.getRound(drwNo);
    return res.data!;
  }

  Future<PageResult<Round>> list({int page = 0, int size = 20}) async {
    final res = await _client.listRounds(page, size);
    return res.data!;
  }
}
