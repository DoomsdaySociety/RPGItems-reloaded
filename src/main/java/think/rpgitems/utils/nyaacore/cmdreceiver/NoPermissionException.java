package think.rpgitems.utils.nyaacore.cmdreceiver;

class NoPermissionException extends RuntimeException {
    /**
     * @param permission name for the permission node
     */
    public NoPermissionException(String permission) {
        super(permission);
    }
}
