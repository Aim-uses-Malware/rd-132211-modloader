package com.mojang.rubydung.modloader;

import com.mojang.rubydung.modloader.api.IMod;
import com.mojang.rubydung.modloader.api.RDMod;

/**
 * Wraps a loaded mod instance with its metadata.
 */
public class ModContainer {

    public enum State { DISCOVERED, PRE_INIT, INIT, POST_INIT, ERROR }

    private final IMod instance;
    private final RDMod meta;
    private State state = State.DISCOVERED;
    private String errorMessage = null;

    public ModContainer(IMod instance, RDMod meta) {
        this.instance = instance;
        this.meta = meta;
    }

    public IMod getInstance() { return instance; }
    public RDMod getMeta()    { return meta; }
    public State getState()   { return state; }
    public String getId()     { return meta.id(); }
    public String getName()   { return meta.name(); }
    public String getVersion(){ return meta.version(); }
    public String getAuthor() { return meta.author(); }
    public boolean hasError() { return state == State.ERROR; }
    public String getError()  { return errorMessage; }

    void setState(State s) { this.state = s; }
    void setError(String msg) {
        this.state = State.ERROR;
        this.errorMessage = msg;
    }

    @Override
    public String toString() {
        return "[" + meta.id() + " v" + meta.version() + " by " + meta.author() + "]";
    }
}
