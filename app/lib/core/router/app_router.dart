import 'package:flutter/material.dart';
import 'package:go_router/go_router.dart';
import 'package:riverpod_annotation/riverpod_annotation.dart';

import '../../features/home/presentation/home_screen.dart';
import '../../features/numbers/presentation/numbers_screen.dart';
import '../../features/rounds/presentation/round_detail_screen.dart';
import '../../features/rounds/presentation/rounds_screen.dart';
import '../../features/saved/presentation/saved_screen.dart';
import '../../features/stats/presentation/stats_screen.dart';
import 'scaffold_with_nav.dart';

part 'app_router.g.dart';

@riverpod
GoRouter appRouter(AppRouterRef ref) {
  return GoRouter(
    initialLocation: '/',
    routes: [
      StatefulShellRoute.indexedStack(
        builder: (context, state, navigationShell) =>
            ScaffoldWithNav(navigationShell: navigationShell),
        branches: [
          StatefulShellBranch(routes: [
            GoRoute(path: '/', builder: (_, __) => const HomeScreen()),
          ]),
          StatefulShellBranch(routes: [
            GoRoute(
              path: '/rounds',
              builder: (_, __) => const RoundsScreen(),
              routes: [
                GoRoute(
                  path: ':drwNo',
                  builder: (_, state) => RoundDetailScreen(
                    drwNo: int.parse(state.pathParameters['drwNo']!),
                  ),
                ),
              ],
            ),
          ]),
          StatefulShellBranch(routes: [
            GoRoute(
              path: '/stats',
              builder: (_, __) => const StatsScreen(),
              routes: [
                GoRoute(
                  path: 'numbers',
                  builder: (_, __) => const NumbersScreen(),
                ),
              ],
            ),
          ]),
          StatefulShellBranch(routes: [
            GoRoute(path: '/saved', builder: (_, __) => const SavedScreen()),
          ]),
        ],
      ),
    ],
  );
}
