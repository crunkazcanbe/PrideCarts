package com.dogpound.pridecarts.carts;

/** Any PrideCarts mini car: knows its {@link CartType} and shows its couplings to the client. */
public interface IPrideCart {
    CartType cartType();

    /** Entity ids of the carts coupled in slot 0/1 (0 = none) and whether that link is rope; synced for the renderer. */
    int linkedId(int slot);

    boolean linkedRope(int slot);

    /** Server: the coupling NBT changed — refresh the synced ids now. */
    void linksChanged();
}
