import Roguelike.*;

class Test{
    final Dungeon map;

    Test(){
        map = new Dungeon();
        map.mapPrint();
    }


    public static void main(String[] args){
        new Test();
    }
}