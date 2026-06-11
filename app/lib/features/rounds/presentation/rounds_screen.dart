import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:go_router/go_router.dart';
import 'package:riverpod_annotation/riverpod_annotation.dart';

import '../data/rounds_repository.dart';
import '../domain/page_result.dart';
import '../domain/round.dart';
import '../../../core/ads/native_ad_widget.dart';
import '../../../core/error/app_exception.dart';

part 'rounds_screen.g.dart';

@riverpod
Future<PageResult<Round>> roundsPage(RoundsPageRef ref, int page) =>
    ref.watch(roundsRepositoryProvider).list(page: page);

class RoundsScreen extends ConsumerStatefulWidget {
  const RoundsScreen({super.key});

  @override
  ConsumerState<RoundsScreen> createState() => _RoundsScreenState();
}

class _RoundsScreenState extends ConsumerState<RoundsScreen> {
  int _page = 0;
  final _searchController = TextEditingController();

  @override
  void dispose() {
    _searchController.dispose();
    super.dispose();
  }

  void _onSearch() {
    final input = _searchController.text.trim();
    final round = int.tryParse(input);
    if (round != null && round > 0) {
      _searchController.clear();
      context.go('/rounds/$round');
    }
  }

  @override
  Widget build(BuildContext context) {
    final async = ref.watch(roundsPageProvider(_page));
    return Scaffold(
      appBar: AppBar(title: const Text('회차 검색')),
      body: Column(
        children: [
          _SearchBar(
            controller: _searchController,
            onSearch: _onSearch,
          ),
          Expanded(
            child: async.when(
              loading: () => const Center(child: CircularProgressIndicator()),
              error: (e, _) => _ErrorView(
                message: errorMessage(e),
                onRetry: () => ref.invalidate(roundsPageProvider(_page)),
              ),
              data: (page) => RefreshIndicator(
                onRefresh: () async =>
                    ref.invalidate(roundsPageProvider(_page)),
                child: Column(
                  children: [
                    Expanded(
                      child: _RoundsList(rounds: page.content),
                    ),
                    _Pagination(
                      current: _page,
                      totalPages: page.totalPages,
                      onChanged: (p) => setState(() => _page = p),
                    ),
                  ],
                ),
              ),
            ),
          ),
        ],
      ),
    );
  }
}

class _SearchBar extends StatelessWidget {
  const _SearchBar({required this.controller, required this.onSearch});
  final TextEditingController controller;
  final VoidCallback onSearch;

  @override
  Widget build(BuildContext context) {
    return Padding(
      padding: const EdgeInsets.fromLTRB(16, 12, 16, 4),
      child: TextField(
        controller: controller,
        keyboardType: TextInputType.number,
        textInputAction: TextInputAction.search,
        onSubmitted: (_) => onSearch(),
        decoration: InputDecoration(
          hintText: '회차 번호로 바로 이동 (예: 1200)',
          prefixIcon: const Icon(Icons.tag),
          suffixIcon: IconButton(
            icon: const Icon(Icons.search),
            onPressed: onSearch,
          ),
          border: const OutlineInputBorder(),
          isDense: true,
          contentPadding:
              const EdgeInsets.symmetric(vertical: 10, horizontal: 12),
        ),
      ),
    );
  }
}

// 5번째 아이템마다 네이티브 광고를 삽입하는 리스트
class _RoundsList extends StatelessWidget {
  const _RoundsList({required this.rounds});
  final List<Round> rounds;

  static const int _adEvery = 5;

  @override
  Widget build(BuildContext context) {
    // 광고 포함 총 아이템 수: rounds.length + 광고 개수
    final adCount = rounds.length ~/ _adEvery;
    final total = rounds.length + adCount;
    return ListView.builder(
      physics: const AlwaysScrollableScrollPhysics(),
      itemCount: total,
      itemBuilder: (_, virtualIndex) {
        // virtualIndex에서 광고 위치인지 판별
        if ((virtualIndex + 1) % (_adEvery + 1) == 0) {
          return const Padding(
            padding: EdgeInsets.symmetric(vertical: 4),
            child: NativeAdWidget(),
          );
        }
        // 실제 rounds 인덱스 계산 (광고 슬롯 제외)
        final adsBefore = virtualIndex ~/ (_adEvery + 1);
        final roundIndex = virtualIndex - adsBefore;
        if (roundIndex >= rounds.length) return const SizedBox.shrink();
        return _RoundTile(round: rounds[roundIndex]);
      },
    );
  }
}

class _RoundTile extends StatelessWidget {
  const _RoundTile({required this.round});
  final Round round;

  @override
  Widget build(BuildContext context) {
    return ListTile(
      title: Text('제 ${round.round}회 (${round.drawDate})'),
      subtitle: Wrap(
        spacing: 4,
        children: round.mainNumbers.map((n) => _Ball(n)).toList(),
      ),
      trailing: const Icon(Icons.chevron_right),
      onTap: () => context.go('/rounds/${round.round}'),
    );
  }
}

class _Ball extends StatelessWidget {
  const _Ball(this.number);
  final int number;

  Color get _color {
    if (number <= 10) return const Color(0xFFF9A825);
    if (number <= 20) return const Color(0xFF42A5F5);
    if (number <= 30) return const Color(0xFFEF5350);
    if (number <= 40) return const Color(0xFF757575);
    return const Color(0xFF66BB6A);
  }

  @override
  Widget build(BuildContext context) {
    return Container(
      width: 28,
      height: 28,
      decoration: BoxDecoration(color: _color, shape: BoxShape.circle),
      alignment: Alignment.center,
      child: Text(
        '$number',
        style: const TextStyle(
          color: Colors.white,
          fontSize: 11,
          fontWeight: FontWeight.bold,
        ),
      ),
    );
  }
}

class _Pagination extends StatelessWidget {
  const _Pagination({
    required this.current,
    required this.totalPages,
    required this.onChanged,
  });
  final int current;
  final int totalPages;
  final ValueChanged<int> onChanged;

  @override
  Widget build(BuildContext context) {
    return Padding(
      padding: const EdgeInsets.symmetric(vertical: 8),
      child: Row(
        mainAxisAlignment: MainAxisAlignment.center,
        children: [
          IconButton(
            icon: const Icon(Icons.chevron_left),
            onPressed: current > 0 ? () => onChanged(current - 1) : null,
          ),
          Text('${current + 1} / $totalPages'),
          IconButton(
            icon: const Icon(Icons.chevron_right),
            onPressed:
                current < totalPages - 1 ? () => onChanged(current + 1) : null,
          ),
        ],
      ),
    );
  }
}

class _ErrorView extends StatelessWidget {
  const _ErrorView({required this.message, required this.onRetry});
  final String message;
  final VoidCallback onRetry;

  @override
  Widget build(BuildContext context) {
    return Center(
      child: Column(
        mainAxisSize: MainAxisSize.min,
        children: [
          Text(message),
          const SizedBox(height: 12),
          FilledButton(onPressed: onRetry, child: const Text('재시도')),
        ],
      ),
    );
  }
}
