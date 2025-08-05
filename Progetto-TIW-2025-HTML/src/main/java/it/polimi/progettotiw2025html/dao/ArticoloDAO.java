package it.polimi.progettotiw2025html.dao;

import it.polimi.progettotiw2025html.utils.ConnectionHandler;
import java.sql.*;
import it.polimi.progettotiw2025html.beans.*;

public class ArticoloDAO {
    private final Connection connection;

    public ArticoloDAO(Connection connection) {
        this.connection = connection;
    }
}