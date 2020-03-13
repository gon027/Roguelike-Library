package Roguelike;

import java.util.*;
import Roguelike.DungeonUtil.*;

public class Dungeon{
    public final DungeonMap dmap;
    public final ArrayList<DungeonRect> mapList;
    private final CreateDungeon cDungeon;

    // 部屋の大きさの最大値と最小値
    private final int MINROOMSIZE = 4;
    private final int MAXROOMSIZE = 6;

    // private final int MINROOMSIZEX = 4;
    // private final int MAXROOMSIZEX = 6;

    private final int MINROOMSIZEY = 4;
    // private final int MAXROOMSIZEY = 5;

    private final int PADDING = 2;

    // 部屋の数
    private int roomCount = 0;

    public Dungeon() {
        this(54, 30);
    }

    public Dungeon(final int _width, final int _height) {
        dmap = new DungeonMap(_width, _height);
        mapList = new ArrayList<DungeonRect>();
        cDungeon = new CreateDungeon();
        createDungeon();
    }

    void createDungeon() {
        init();
        divisionMap(mapList.get(0));
        createRoom();
        // rectShow();
        cDungeon.digRoad(this, dmap);
        // createRoad();
        // for (DungeonRect rect : mapList) {
        // System.out.println(rect);
        // }
    }

    void init() {
        roomCount = 0;
        dmap.mapClear();
        mapList.clear();
        mapList.add(new DungeonRect());
        mapList.get(0).setRect(0, 0, dmap.WIDTH, dmap.HEIGHT, roomCount);
    }

    void divisionMap(final DungeonRect _r) {
        if (roomCount == 8 - 1) {
            return;
        }

        final boolean hvFrag = RandomUtil.rand.nextBoolean();

        if (hvFrag) {
            verticalSplit(_r); // 縦に分割
        } else {
            horizontalSplit(_r); // 横に分割
        }
    }

    // 縦
    void verticalSplit(final DungeonRect _r) {
        if (_r.area.getWidth() <= (MINROOMSIZE + 3) * 2 + 5) {
            return;
        }

        // 分割点
        int div = _r.area.getWidth() - (MINROOMSIZE * 2) - (PADDING * 2);
        div = Math.min(div, MAXROOMSIZE);
        final int left = (_r.area.left + 8) + RandomUtil.getRandomRange(0, div);

        roomCount++;
        final DungeonRect child = new DungeonRect();
        child.setRect(left, _r.area.top, _r.area.right, _r.area.bottom, roomCount);
        mapList.add(child);

        _r.area.setRectAngle(_r.area.left, _r.area.top, left + 1, _r.area.bottom);

        divisionMap(mapList.get(mapList.size() - 1));
        divisionMap(_r);
    }

    // 横
    void horizontalSplit(final DungeonRect _r) {
        // (3 + 3) * 2 + 3 = 6 * 2 + 3 = 12 + 3 = 15
        if (_r.area.getHeight() <= (MINROOMSIZEY + 3) * 2 + 1) {
            return;
        }

        int div = _r.area.getHeight() - (MINROOMSIZE * 2) - 4;
        div = Math.min(div, MAXROOMSIZE);

        // System.out.println(div);
        final int top = (_r.area.top + 7) + RandomUtil.getRandomRange(0, div);

        roomCount++;
        final DungeonRect child = new DungeonRect();
        child.setRect(_r.area.left, top, _r.area.right, _r.area.bottom, roomCount);
        mapList.add(child);

        _r.area.setRectAngle(_r.area.left, _r.area.top, _r.area.right, top + 1);

        divisionMap(mapList.get(mapList.size() - 1));
        divisionMap(_r);
    }

    void createRoom() {
        for (final var e : mapList) {
            // 部屋の大きさを求める
            // 壁, 空き, 分割戦(1, 1, 1)
            final int width = e.area.getWidth() - 2;
            final int height = e.area.getHeight() - 2;

            // 部屋の大きさを[MIN, w / h]の範囲で決める
            final int rx = RandomUtil.getRandomRange(MINROOMSIZE, width - 1);
            final int ry = RandomUtil.getRandomRange(MINROOMSIZE, height) - 1;

            // 空きサイズ(区間 - 部屋)
            final int fx = (width - rx);
            final int fy = (height - ry);

            // 部屋の左上の位置
            final int lux = RandomUtil.getRandomRange(1, fx) + 1;
            final int luy = RandomUtil.getRandomRange(1, fy) + 1;

            final int sx = e.area.left + lux;
            final int gx = sx + rx;
            final int sy = e.area.top + luy;
            final int gy = sy + ry;

            e.setRoom(sx, sy, gx, gy);

            dmap.fillValue(sx, sy, gx, gy, MapChip.MAP_NONE);
        }
    }

    void checkConectRoom() {
        DungeonRect target, next;
        // 繋ぐ部屋を調べる
        for (int i = 0; i < mapList.size(); i++) {
            target = mapList.get(i);
            for (int j = 0; j < mapList.size(); j++) {
                if (i == j)
                    continue;

                next = mapList.get(j);
                if (target.area.left == next.area.right - 1 || target.area.top == next.area.bottom - 1
                        || target.area.right - 1 == next.area.left || target.area.bottom - 1 == next.area.top) {
                    target.nextRoomID.add(next.roomID);
                }
            }
        }

        for (int i = 0; i < mapList.size(); i++) {
            target = mapList.get(i);
            Collections.shuffle(target.nextRoomID);
        }
    }

    void createRoad() {
        checkConectRoom();

        int nowPosx = 0;
        int nowPosy = 0;
        int targetPosx = 0;
        int targetPosy = 0;
        int midx = 0;
        int midy = 0;

        for (int i = 0; i < mapList.size(); i++) {
            final DungeonRect now = mapList.get(i);

            for (int j = 0; j < now.nextRoomID.size(); j++) {
                final DungeonRect target = mapList.get(now.nextRoomID.get(j));

                if (now.isConected && target.isConected) {
                    continue;
                }

                if (!isHorizontallyAjeacent(now, target)) {
                    // 縦に掘る
                    if (now.room.top > target.room.bottom) {
                        // nowがした
                        nowPosx = RandomUtil.getRandomRange(now.room.left, now.room.right);
                        nowPosy = now.room.top;
                        targetPosx = RandomUtil.getRandomRange(target.room.left, target.room.right);
                        targetPosy = target.room.bottom;
                        midy = now.room.top;
                    } else {
                        nowPosx = RandomUtil.getRandomRange(target.room.left, target.room.right);
                        nowPosy = target.room.top;
                        targetPosx = RandomUtil.getRandomRange(now.room.left, now.room.right);
                        targetPosy = now.room.bottom;
                        midy = target.room.top;
                    }

                    // 道の補正
                    if (dmap.getMapChip(nowPosx - 1, nowPosy - 1) == MapChip.MAP_NONE) {
                        nowPosx -= 1;
                    } else if (dmap.getMapChip(nowPosx + 1, nowPosy - 1) == MapChip.MAP_NONE) {
                        nowPosx += 1;
                    }

                    if (dmap.getMapChip(targetPosx - 1, targetPosy) == MapChip.MAP_NONE) {
                        targetPosx -= 1;
                    } else if (dmap.getMapChip(targetPosx + 1, targetPosy) == MapChip.MAP_NONE) {
                        targetPosx += 1;
                    }

                    fillVLine(targetPosy, midy + 1, targetPosx, MapChip.MAP_NONE);
                    fillVLine(midy, nowPosy, nowPosx, MapChip.MAP_NONE);
                    fillHLine(nowPosx, targetPosx, midy, MapChip.MAP_NONE);

                } else {
                    // 横に掘る
                    if (now.room.right < target.room.left) {
                        // originが左
                        nowPosx = now.room.right;
                        nowPosy = RandomUtil.getRandomRange(now.room.top, now.room.bottom);
                        targetPosx = target.room.left;
                        targetPosy = RandomUtil.getRandomRange(target.room.top, target.room.bottom);
                        midx = now.room.right - 1;
                    } else {
                        nowPosx = target.room.right;
                        nowPosy = RandomUtil.getRandomRange(target.room.top, target.room.bottom);

                        targetPosx = now.room.left;
                        targetPosy = RandomUtil.getRandomRange(now.room.top, now.room.bottom);
                        midx = target.room.right - 1;
                    }

                    // 道の補正
                    if (dmap.getMapChip(nowPosx, nowPosy - 1) == MapChip.MAP_NONE) {
                        nowPosy -= 1;
                    } else if (dmap.getMapChip(nowPosx, nowPosy + 1) == MapChip.MAP_NONE) {
                        nowPosy += 1;
                    }

                    if (dmap.getMapChip(targetPosx - 1, targetPosy - 1) == MapChip.MAP_NONE) {
                        targetPosy -= 1;
                    } else if (dmap.getMapChip(targetPosx - 1, targetPosy + 1) == MapChip.MAP_NONE) {
                        targetPosy += 1;
                    }

                    fillHLine(nowPosx, midx + 1, nowPosy, MapChip.MAP_NONE);
                    fillHLine(midx, targetPosx, targetPosy, MapChip.MAP_NONE);
                    fillVLine(nowPosy, targetPosy + 1, midx, MapChip.MAP_NONE);
                }

                now.isConected = target.isConected = true;
            }
        }
    }

    // 矩形が横に接しているか
    boolean isHorizontallyAjeacent(final DungeonRect now, final DungeonRect target) {
        if (now.area.right - 1 == target.area.left || now.area.left + 1 == target.area.right) {
            return true;
        }
        return false;
    }

    public void fillHLine(int _left, int _right, final int _y, final int _value) {
        if (_left > _right) {
            final int t = _left;
            _left = _right;
            _right = t;
        }

        dmap.fillValue(_left, _y, _right, _y + 1, _value);
    }

    public void fillVLine(int _top, int _bottom, final int _x, final int _value) {
        if (_top > _bottom) {
            final int t = _top;
            _top = _bottom;
            _bottom = t;
        }

        dmap.fillValue(_x, _top, _x + 1, _bottom, _value);
    }

    void rectShow() {
        for (var e : mapList) {
            for (int y = e.area.top; y < e.area.bottom; y++) {
                for (int x = e.area.left; x < e.area.right; x++) {
                    if (y == e.area.top || y == e.area.bottom - 1 || x == e.area.left || x == e.area.right - 1) {
                        dmap.setValue(x, y, 2);
                    }
                }
            }
        }
    }

}