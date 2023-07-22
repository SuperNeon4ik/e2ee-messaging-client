package me.superneon4ik.enums;

import lombok.Getter;

public enum PacketID {
    PUBKEY(0),
    MESSAGE(1),
    GOODBYE(2),
    CATCHUP(3);

    @Getter private int id;
    PacketID(int id) {
        this.id = id;
    }
}
