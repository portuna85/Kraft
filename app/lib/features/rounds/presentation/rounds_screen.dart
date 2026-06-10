import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:go_router/go_router.dart';
import 'package:riverpod_annotation/riverpod_annotation.dart';

import '../data/rounds_repository.dart';
import '../domain/page_result.dart';
import '../domain/round.dart';
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

  @override
  Widget build(BuildContext context) {
    final async = ref.watch(roundsPageProvider(_page));
    return Scaffold(
      appBar: AppBar(title: const Text('회차 검색')),
      body: async.when(
        loading: () => const Center(child: CircularProgressIndicator()),
        error: (e, _) => _ErrorView(
          message: e is AppException ? e.message : '오류가 발생했습니다',
          onRetry: () => ref.invalidate(roundsPageProvider(_page)),
        ),
        data: (page) => Column(
          children: [
            Expanded(
              child: ListView.builder(
                itemCount: page.content.length,
                itemBuilder: (_, i) => _RoundTile(round: page.content[i]),
              ),
            ),
            _Pagination(
              current: _page,
              totalPages: page.totalPages,
              onChanged: (p) => setState(() => _page = p),
            ),
          ],
        ),
      ),
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
            onPressed: current < totalPages - 1 ? () => onChanged(current + 1) : null,
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
