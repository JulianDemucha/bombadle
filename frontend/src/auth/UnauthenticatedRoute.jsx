import React from "react";
import {Navigate} from "react-router-dom";
import {useAuth} from "./UseAuth.jsx";

export default function UnauthenticatedRoute({children}) {
    const {user, loading} = useAuth();

    if (loading) return <p>Sprawdzanie autoryzacji...</p>;
    if (user) return <Navigate to="/"/>;

    return children;
}
