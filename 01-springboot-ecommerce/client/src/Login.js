import React, {Component} from "react";
import {Button, FormControl, FormGroup, FormLabel} from "react-bootstrap";
import "./Login.css";
import {ACCESS_TOKEN, BASE_URL} from "./config/config";
import Alert from "react-bootstrap/Alert";
import axios from "axios";

export default class Login extends Component {
    constructor(props) {
        super(props);

        this.state = {
            username: "",
            password: "",
            error: null
        };
    }

    validateForm() {
        return this.state.username.length > 0 && this.state.password.length > 0;
    }

    handleChange = event => {
        this.setState({
            [event.target.id]: event.target.value
        });
    };

    handleSubmit = event => {
        event.preventDefault();

        const payload = {
            "username": this.state.username,
            "password": this.state.password
        };

        axios.post(BASE_URL + '/login', payload).then(response => {
            localStorage.setItem(ACCESS_TOKEN, response.data.accessToken);
            this.setState({
                error: null
            });
            this.props.history.push("/profile");
        }).catch(error => {
            localStorage.setItem(ACCESS_TOKEN, null);
            this.setState({
                error: "Username or password is incorrect",
            });
        });
    };

    render() {
        const errorMessage = this.state.error ? (
            <Alert variant="danger" className="error-message">{this.state.error}</Alert>) : null;

        return (
            <div className="Login">
                <form onSubmit={this.handleSubmit}>
                    {errorMessage}
                    <FormGroup controlId="username" bssize="large">
                        <FormLabel>Username</FormLabel>
                        <FormControl autoFocus type="username" value={this.state.username}
                                     onChange={this.handleChange}/>
                    </FormGroup>
                    <FormGroup controlId="password" bssize="large">
                        <FormLabel>Password</FormLabel>
                        <FormControl value={this.state.password} onChange={this.handleChange} type="password"
                                     autoComplete="new-password"/>
                    </FormGroup>
                    <Button block bssize="large" disabled={!this.validateForm()} type="submit">
                        Login
                    </Button>
                </form>
            </div>
        );
    }
}