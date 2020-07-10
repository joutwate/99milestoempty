import React, {Component} from "react";
import {Button, Col, FormControl, FormGroup, FormLabel, Row} from "react-bootstrap";
import "./AccountRegistration.css";
import {BASE_URL} from "../config/config";
import Alert from "react-bootstrap/Alert";
import axios from "axios";

export default class AccountRegistration extends Component {
    constructor(props) {
        super(props);

        this.state = {
            firstName: "",
            lastName: "",
            username: "",
            email: "",
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
            "firstName": this.state.firstName,
            "lastName": this.state.lastName,
            "username": this.state.username,
            "email": this.state.email,
            "password": this.state.password
        };

        axios.post(BASE_URL + '/register', payload).then(response => {
            this.props.history.push("/login");
        }).catch(error => {
            console.log(error.response);
        });
    };

    render() {
        const errorMessage = this.state.error ? (
            <Alert variant="danger" className="error-message">{this.state.error}</Alert>) : null;

        return (
            <div className="AccountRegistration">
                <form onSubmit={this.handleSubmit}>
                    {errorMessage}
                    <Row>
                        <FormGroup as={Col} controlId="firstName" bssize="large">
                            <FormLabel>First Name</FormLabel>
                            <FormControl autoFocus type="text" value={this.state.firstName}
                                         onChange={this.handleChange}/>
                        </FormGroup>
                    </Row>
                    <Row>
                        <FormGroup as={Col} controlId="lastName" bssize="large">
                            <FormLabel>Last Name</FormLabel>
                            <FormControl type="text" value={this.state.lastName}
                                         onChange={this.handleChange}/>
                        </FormGroup>
                    </Row>
                    <Row>
                        <FormGroup as={Col} controlId="email" bssize="large">
                            <FormLabel>Email Address</FormLabel>
                            <FormControl type="email" value={this.state.email}
                                         onChange={this.handleChange}/>
                        </FormGroup>
                    </Row>
                    <Row>
                        <FormGroup as={Col} controlId="username" bssize="large">
                            <FormLabel>Username</FormLabel>
                            <FormControl type="text" autoComplete="username" value={this.state.username}
                                         onChange={this.handleChange}/>
                        </FormGroup>
                    </Row>
                    <Row>
                        <FormGroup as={Col} controlId="password" bssize="large">
                            <FormLabel>Password</FormLabel>
                            <FormControl value={this.state.password} onChange={this.handleChange} type="password"
                                         autoComplete="new-password"/>
                        </FormGroup>
                    </Row>
                    <Button bssize="large" disabled={!this.validateForm()} type="submit">
                        Register
                    </Button>
                </form>
            </div>
        );
    }
}